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

/**
 * Hierarchy of recoverable errors that [SecureVault] operations may raise.
 *
 * Every public [SecureVault] method either succeeds or throws a [VaultException];
 * platform-specific errors are wrapped so callers can write cross-platform
 * `try`/`catch` without depending on `java.*` or `platform.Security.*`.
 *
 * @since 0.1.0
 */
public sealed class VaultException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    /** The supplied key was blank or otherwise illegal. */
    public class InvalidKey(key: String) :
        VaultException("Invalid key: '$key'. Keys must be non-blank.")

    /** A cryptographic primitive (cipher, key-derivation, AEAD) failed. */
    public class CryptoFailure(cause: Throwable) :
        VaultException("Cryptographic operation failed: ${cause.message}", cause)

    /**
     * The underlying ciphertext failed an integrity check — the value has been
     * tampered with, the master key has rotated, or the data was written by a
     * different app/installation.
     */
    public class Tampered(cause: Throwable) :
        VaultException("Stored value failed integrity check", cause)

    /**
     * The backend (Keystore / Keychain / SharedPreferences file) could not be
     * reached. Typically transient — retry after the device is unlocked or the
     * user has authenticated.
     */
    public class StorageUnavailable(cause: Throwable? = null) :
        VaultException("Secure storage backend unavailable", cause)
}

