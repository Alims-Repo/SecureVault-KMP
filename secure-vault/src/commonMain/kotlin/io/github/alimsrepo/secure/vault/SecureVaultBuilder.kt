/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:JvmName("SecureVaults")

package io.github.alimsrepo.secure.vault

import kotlin.jvm.JvmName

/**
 * Builds a [SecureVault] for the given [config].
 *
 * Canonical entry point as of `0.2.0`. The call site is identical on every
 * supported platform — no `Context`, no factory, no two-step dance:
 *
 * ```kotlin
 * // commonMain — works on Android and iOS:
 * val vault = SecureVault(VaultConfig(namespace = "com.acme.auth"))
 * ```
 *
 * ### Android initialisation
 *
 * The Android backend needs an `Application` `Context`. By default that
 * context is captured automatically by an
 * [`androidx.startup`](https://developer.android.com/topic/libraries/app-startup)
 * `Initializer` registered in the library's `AndroidManifest.xml`, so most
 * consumers do not need to do anything. If you have intentionally disabled
 * the auto-initialisation hook, call `SecureVault.initialize(context)` once
 * from `Application.onCreate()` before the first `SecureVault(...)` call.
 *
 * ### Threading
 *
 * Construction itself is cheap. **The first interaction** with the returned
 * vault (e.g. `keys()`, `get(...)`) may block briefly while the platform
 * derives or unwraps the master key — Android Keystore on first launch is
 * typically the slowest. Issue one read on a background dispatcher during
 * app start-up if you need to keep the UI thread free.
 *
 * Multiple calls with the same [VaultConfig.namespace] return independent
 * instances that address the same underlying storage.
 *
 * @since 0.2.0
 */
public expect fun SecureVault(config: VaultConfig): SecureVault

/**
 * Convenience overload — equivalent to
 * `SecureVault(VaultConfig(namespace, accessibility))`.
 *
 * @since 0.2.0
 */
public fun SecureVault(
    namespace: String,
    accessibility: Accessibility = Accessibility.AfterFirstUnlock,
): SecureVault = SecureVault(VaultConfig(namespace = namespace, accessibility = accessibility))

