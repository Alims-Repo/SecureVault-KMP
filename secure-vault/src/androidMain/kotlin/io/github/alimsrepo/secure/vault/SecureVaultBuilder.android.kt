/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault

import android.content.Context
import io.github.alimsrepo.secure.vault.internal.SecureVaultStartup

// NOTE: do *not* re-declare `@file:JvmName("SecureVaults")` here. The expect
// declaration in commonMain already pins the Java facade name to `SecureVaults`
// and the actual function inherits it. Duplicating the annotation on this file
// makes K2 emit two source files into the same JVM class and fail with
// "Duplicate JVM class name 'SecureVaults'".

/** Android `actual` for the top-level [SecureVault] factory. */
public actual fun SecureVault(config: VaultConfig): SecureVault =
    AndroidSecureVault(SecureVaultStartup.requireContext(), config)

/**
 * Manually supplies the application [Context] to the SecureVault library.
 *
 * **You do not normally need to call this.** The library ships an
 * `androidx.startup` initializer that captures the context before
 * `Application.onCreate()` returns. Call this only if you have disabled the
 * Startup integration (e.g. removed the `<meta-data>` entry from a merged
 * manifest, or run in a process where Startup does not fire).
 *
 * Idempotent.
 *
 * @since 0.2.0
 */
public fun SecureVault.Companion.initialize(context: Context) {
    SecureVaultStartup.ensureInitialized(context)
}

