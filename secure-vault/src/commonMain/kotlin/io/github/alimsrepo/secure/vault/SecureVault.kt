/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.alimsrepo.secure.vault

import kotlin.coroutines.cancellation.CancellationException

/**
 * A small, coroutine-first façade over the platform's native secure storage:
 *
 * * **Android** — [EncryptedSharedPreferences](https://developer.android.com/topic/security/data)
 *   backed by an Android Keystore master key (AES-256 GCM for values, AES-256 SIV for keys).
 * * **iOS** — Keychain Services (`kSecClassGenericPassword`).
 *
 * Instances are obtained from [SecureVaultFactory] and are safe to share across
 * coroutines. All suspending functions are I/O bound and dispatch onto an
 * appropriate background dispatcher; callers do not need to switch contexts.
 *
 * Every method that interacts with the backend declares [VaultException]; callers
 * should treat any other [Throwable] as a programming error.
 *
 * @since 0.1.0
 */
public interface SecureVault {

    /**
     * Stores [value] under [key], overwriting any previous value.
     *
     * @throws VaultException.InvalidKey         if [key] is blank.
     * @throws VaultException.CryptoFailure      if encryption fails.
     * @throws VaultException.StorageUnavailable if the backend cannot be reached.
     */
    @Throws(VaultException::class, CancellationException::class)
    public suspend fun put(key: String, value: String)

    /**
     * Returns the value previously stored under [key], or `null` if absent.
     *
     * @throws VaultException.InvalidKey         if [key] is blank.
     * @throws VaultException.CryptoFailure      if decryption fails.
     * @throws VaultException.Tampered           if the ciphertext failed an integrity check.
     * @throws VaultException.StorageUnavailable if the backend cannot be reached.
     */
    @Throws(VaultException::class, CancellationException::class)
    public suspend fun get(key: String): String?

    /**
     * Removes the entry associated with [key]. No-op if absent.
     *
     * @throws VaultException.InvalidKey         if [key] is blank.
     * @throws VaultException.StorageUnavailable if the backend cannot be reached.
     */
    @Throws(VaultException::class, CancellationException::class)
    public suspend fun remove(key: String)

    /**
     * Returns `true` iff a value is currently stored under [key].
     *
     * @throws VaultException.InvalidKey         if [key] is blank.
     * @throws VaultException.StorageUnavailable if the backend cannot be reached.
     */
    @Throws(VaultException::class, CancellationException::class)
    public suspend fun contains(key: String): Boolean

    /**
     * Removes every entry inside this vault's namespace.
     *
     * @throws VaultException.StorageUnavailable if the backend cannot be reached.
     */
    @Throws(VaultException::class, CancellationException::class)
    public suspend fun clear()

    /**
     * Returns a snapshot of every key currently stored in this vault's namespace.
     *
     * The returned set is a defensive copy; mutating it has no effect on the vault.
     *
     * @throws VaultException.StorageUnavailable if the backend cannot be reached.
     */
    @Throws(VaultException::class, CancellationException::class)
    public suspend fun keys(): Set<String>
}

