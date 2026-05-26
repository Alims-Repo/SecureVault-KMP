/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.alimsrepo.secure.vault.internal.requireValidKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException

/**
 * [SecureVault] backed by Jetpack Security's [EncryptedSharedPreferences].
 *
 * * **Keys** are encrypted with AES-256 SIV (deterministic — required so we can
 *   look entries up by name).
 * * **Values** are encrypted with AES-256 GCM (authenticated, randomised IV).
 * * The wrapping key is stored in the Android Keystore under the alias
 *   [MasterKey.DEFAULT_MASTER_KEY_ALIAS]; rotating it invalidates the file,
 *   which we surface as [VaultException.Tampered].
 *
 * The prefs handle is created lazily on first I/O so that constructor calls
 * stay non-blocking. All public methods dispatch onto [Dispatchers.IO].
 */
internal class AndroidSecureVault(
    private val appContext: Context,
    private val config: VaultConfig,
) : SecureVault {

    private val fileName: String = PREFIX + config.namespace
    private val initMutex = Mutex()

    @Volatile private var prefs: SharedPreferences? = null

    private suspend fun prefs(): SharedPreferences {
        prefs?.let { return it }
        return initMutex.withLock {
            prefs ?: withContext(Dispatchers.IO) {
                runCatchingStorage {
                    val masterKey = MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        appContext,
                        fileName,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    )
                }.also { prefs = it }
            }
        }
    }

    override suspend fun put(key: String, value: String) {
        requireValidKey(key)
        withContext(Dispatchers.IO) {
            runCatchingStorage {
                prefs().edit().putString(key, value).apply()
            }
        }
    }

    override suspend fun get(key: String): String? {
        requireValidKey(key)
        return withContext(Dispatchers.IO) {
            runCatchingStorage { prefs().getString(key, null) }
        }
    }

    override suspend fun remove(key: String) {
        requireValidKey(key)
        withContext(Dispatchers.IO) {
            runCatchingStorage { prefs().edit().remove(key).apply() }
        }
    }

    override suspend fun contains(key: String): Boolean {
        requireValidKey(key)
        return withContext(Dispatchers.IO) {
            runCatchingStorage { prefs().contains(key) }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            runCatchingStorage { prefs().edit().clear().apply() }
        }
    }

    override suspend fun keys(): Set<String> = withContext(Dispatchers.IO) {
        runCatchingStorage { prefs().all.keys.toSet() }
    }

    /**
     * Translates platform exceptions into [VaultException] subtypes. Kept inline
     * so we never leak `java.*` types across the public API.
     */
    private inline fun <T> runCatchingStorage(block: () -> T): T =
        try {
            block()
        } catch (e: AEADBadTagException) {
            throw VaultException.Tampered(e)
        } catch (e: GeneralSecurityException) {
            throw VaultException.CryptoFailure(e)
        } catch (e: IOException) {
            throw VaultException.StorageUnavailable(e)
        }

    private companion object {
        /** Prefix isolates our files from any other SharedPreferences the host app uses. */
        const val PREFIX = "secure_vault__"
    }
}

