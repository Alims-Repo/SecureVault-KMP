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
 * Platform entry point for building [SecureVault] instances.
 *
 * @since 0.1.0
 */
@Deprecated(
    message = "Use the top-level SecureVault(config) factory function. " +
        "On Android the application Context is captured automatically by " +
        "androidx.startup — no factory needed. SecureVaultFactory will be " +
        "removed in 0.3.0.",
    replaceWith = ReplaceWith(
        "SecureVault(config)",
        "io.github.alimsrepo.secure.vault.SecureVault",
    ),
    level = DeprecationLevel.WARNING,
)
public expect class SecureVaultFactory {

    /**
     * Builds a [SecureVault] for the given [config]. Multiple calls with the
     * same [VaultConfig.namespace] return independent instances that address
     * the same underlying storage.
     */
    public fun create(config: VaultConfig): SecureVault
}

