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
 * The `actual` declaration on each platform supplies whatever environment the
 * native backend needs (e.g. an Android `Context`) so that common code can
 * stay platform-agnostic.
 *
 * ### Android
 * ```kotlin
 * val factory = SecureVaultFactory(context)
 * val vault   = factory.create(VaultConfig(namespace = "com.acme.auth"))
 * ```
 *
 * ### iOS (Kotlin)
 * ```kotlin
 * val vault = SecureVaultFactory().create(VaultConfig(namespace = "com.acme.auth"))
 * ```
 *
 * ### iOS (Swift)
 * ```swift
 * let vault = SecureVaultFactory().create(
 *     config: VaultConfig(namespace: "com.acme.auth", accessibility: .afterFirstUnlock)
 * )
 * ```
 *
 * @since 0.1.0
 */
public expect class SecureVaultFactory {

    /**
     * Builds a [SecureVault] for the given [config]. Multiple calls with the
     * same [VaultConfig.namespace] return independent instances that address
     * the same underlying storage.
     */
    public fun create(config: VaultConfig): SecureVault
}

