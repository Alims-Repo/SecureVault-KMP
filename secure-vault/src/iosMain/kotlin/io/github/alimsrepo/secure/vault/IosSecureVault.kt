/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.github.alimsrepo.secure.vault

import io.github.alimsrepo.secure.vault.internal.requireValidKey
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDecode
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitAll
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus

/**
 * [SecureVault] backed by iOS Keychain Services using `kSecClassGenericPassword`
 * items, scoped per [VaultConfig.namespace] via `kSecAttrService`.
 *
 * The Keychain is an OS-managed encrypted store; no application-level
 * cryptography is performed here. [VaultConfig.accessibility] is mapped onto
 * the `kSecAttrAccessible*` family using the `*ThisDeviceOnly` variants so
 * secrets are never restored to a different device.
 *
 * Queries are built as `NSMutableDictionary` instances and cast to
 * `CFDictionaryRef` via toll-free bridging — the canonical Kotlin/Native
 * pattern for the Keychain APIs.
 */
internal class IosSecureVault(
    private val config: VaultConfig,
) : SecureVault {

    private val service: String = config.namespace

    /**
     * Invalidation broker shared by every [IosSecureVault] that targets the
     * same `service` (namespace). The Keychain has no native change-notification
     * API for this process — we approximate it by emitting after every write
     * performed *through this library*. Writes from another process or via raw
     * `SecItem*` calls outside this library are not observed (documented in
     * [SecureVault.observe]).
     */
    private val invalidations: MutableSharedFlow<Unit> = InvalidationBroker.forService(service)

    override suspend fun put(key: String, value: String): Unit = withContext(Dispatchers.Default) {
        requireValidKey(key)
        val data = value.toNSData()

        val update = SecItemUpdate(
            query = baseQuery(account = key).asCF(),
            attributesToUpdate = NSMutableDictionary().apply {
                setObject(data, forKey = kSecValueData.asNSCopyingKey())
            }.asCF(),
        )
        when (update) {
            errSecSuccess -> Unit
            errSecItemNotFound -> {
                val attrs = baseQuery(account = key).apply {
                    setObject(config.accessibility.cfValue, forKey = kSecAttrAccessible.asNSCopyingKey())
                    setObject(data, forKey = kSecValueData.asNSCopyingKey())
                }
                SecItemAdd(attrs.asCF(), null).requireSuccess()
            }
            else -> update.requireSuccess()
        }
        invalidations.tryEmit(Unit)
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.Default) {
        requireValidKey(key)
        memScoped {
            val out = alloc<CFTypeRefVar>()
            val query = baseQuery(account = key).apply {
                setObject(kCFBooleanTrue, forKey = kSecReturnData.asNSCopyingKey())
                setObject(kSecMatchLimitOne, forKey = kSecMatchLimit.asNSCopyingKey())
            }
            when (val status = SecItemCopyMatching(query.asCF(), out.ptr)) {
                errSecSuccess -> (CFBridgingRelease(out.value) as? NSData)?.toUtf8String()
                errSecItemNotFound -> null
                else -> { status.requireSuccess(); null }
            }
        }
    }

    override suspend fun remove(key: String): Unit = withContext(Dispatchers.Default) {
        requireValidKey(key)
        val status = SecItemDelete(baseQuery(account = key).asCF())
        if (status != errSecSuccess && status != errSecItemNotFound) status.requireSuccess()
        invalidations.tryEmit(Unit)
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.Default) {
        requireValidKey(key)
        when (val status = SecItemCopyMatching(baseQuery(account = key).asCF(), null)) {
            errSecSuccess -> true
            errSecItemNotFound -> false
            else -> { status.requireSuccess(); false }
        }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.Default) {
        val status = SecItemDelete(serviceScopedQuery().asCF())
        if (status != errSecSuccess && status != errSecItemNotFound) status.requireSuccess()
        invalidations.tryEmit(Unit)
    }

    override suspend fun keys(): Set<String> = withContext(Dispatchers.Default) {
        memScoped {
            val out = alloc<CFTypeRefVar>()
            val query = serviceScopedQuery().apply {
                setObject(kCFBooleanTrue, forKey = kSecReturnAttributes.asNSCopyingKey())
                setObject(kSecMatchLimitAll, forKey = kSecMatchLimit.asNSCopyingKey())
            }
            when (val status = SecItemCopyMatching(query.asCF(), out.ptr)) {
                errSecSuccess -> {
                    @Suppress("UNCHECKED_CAST")
                    val list = CFBridgingRelease(out.value) as? List<Map<Any?, Any?>>
                    list.orEmpty()
                        .mapNotNull { it[kSecAttrAccount] as? String }
                        .toSet()
                }
                errSecItemNotFound -> emptySet()
                else -> { status.requireSuccess(); emptySet() }
            }
        }
    }

    override fun observe(key: String): Flow<String?> = flow {
        requireValidKey(key)
        emit(get(key))
        invalidations.collect { emit(get(key)) }
    }.distinctUntilChanged()

    override fun observeKeys(): Flow<Set<String>> = flow {
        emit(keys())
        invalidations.collect { emit(keys()) }
    }.distinctUntilChanged()

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun baseQuery(account: String): NSMutableDictionary = NSMutableDictionary().apply {
        setObject(kSecClassGenericPassword!!, forKey = kSecClass.asNSCopyingKey())
        setObject(service as NSString, forKey = kSecAttrService.asNSCopyingKey())
        setObject(account as NSString, forKey = kSecAttrAccount.asNSCopyingKey())
    }

    private fun serviceScopedQuery(): NSMutableDictionary = NSMutableDictionary().apply {
        setObject(kSecClassGenericPassword!!, forKey = kSecClass.asNSCopyingKey())
        setObject(service as NSString, forKey = kSecAttrService.asNSCopyingKey())
    }

    private val Accessibility.cfValue: CFTypeRef
        get() = when (this) {
            Accessibility.AfterFirstUnlock -> kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly!!
            Accessibility.WhenUnlocked -> kSecAttrAccessibleWhenUnlockedThisDeviceOnly!!
        }

    private fun OSStatus.requireSuccess() {
        if (this == errSecSuccess) return
        val msg = "Keychain error: OSStatus=$this"
        throw when (this) {
            errSecDecode -> VaultException.Tampered(RuntimeException(msg))
            errSecDuplicateItem -> VaultException.CryptoFailure(RuntimeException(msg))
            else -> VaultException.StorageUnavailable(RuntimeException(msg))
        }
    }
}

// ------------------------------------------------------------------
// File-private bridging helpers (toll-free CF <-> Foundation)
// ------------------------------------------------------------------

@Suppress("UNCHECKED_CAST")
private fun NSDictionary.asCF(): CFDictionaryRef? = this as CFDictionaryRef?

/**
 * Keychain attribute constants are typed as `CFStringRef?` in cinterop bindings;
 * they are toll-free bridged to `NSString`, which conforms to `NSCopying` and
 * is therefore valid as an NSDictionary key.
 */
@Suppress("UNCHECKED_CAST")
private fun CFTypeRef?.asNSCopyingKey(): NSCopyingProtocol = this as NSCopyingProtocol

private fun String.toNSData(): NSData =
    (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        ?: throw VaultException.CryptoFailure(RuntimeException("UTF-8 encode failed"))

private fun NSData.toUtf8String(): String? =
    NSString.create(data = this, encoding = NSUTF8StringEncoding) as String?

