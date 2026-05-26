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
 * Controls when stored values may be decrypted.
 *
 * Maps to Keychain `kSecAttrAccessible*` constants on iOS and to user-presence
 * requirements on Android Keystore. Choose the strictest setting your UX allows.
 *
 * @since 0.1.0
 */
public enum class Accessibility {

    /**
     * Values are readable after the device has been unlocked **at least once**
     * since boot. Survives screen lock — suitable for background work.
     *
     * Maps to `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` on iOS.
     */
    AfterFirstUnlock,

    /**
     * Values are readable **only while the device is currently unlocked**.
     * Highest practical security for routine secrets; the OS may evict cached
     * keys when the screen locks.
     *
     * Maps to `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` on iOS.
     */
    WhenUnlocked,
}

/**
 * Immutable configuration for a [SecureVault] instance.
 *
 * Two vaults built with the same [namespace] address the same data; two vaults
 * with different [namespace]s are isolated from each other (and from any other
 * EncryptedSharedPreferences / Keychain entries this app uses).
 *
 * @property namespace     Logical bucket name. Reverse-DNS is recommended, e.g.
 *                         `"com.acme.app.auth"`. Must match [NAMESPACE_REGEX].
 * @property accessibility When the OS is allowed to decrypt entries. See [Accessibility].
 *
 * @since 0.1.0
 */
public data class VaultConfig(
    val namespace: String,
    val accessibility: Accessibility = Accessibility.AfterFirstUnlock,
) {
    init {
        require(namespace.isNotBlank()) { "namespace must not be blank" }
        require(NAMESPACE_REGEX.matches(namespace)) {
            "namespace '$namespace' must match $NAMESPACE_REGEX"
        }
    }

    public companion object {
        /** Allowed characters and length for [namespace]. */
        public val NAMESPACE_REGEX: Regex = Regex("[A-Za-z0-9._-]{1,64}")
    }
}

