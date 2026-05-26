/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault.internal

import android.content.Context
import androidx.startup.Initializer

/**
 * Captures the application `Context` once at process start so the rest of the
 * library can resolve it without the consumer having to pass it.
 *
 * Wired into `androidx.startup` via the `<meta-data>` entry in
 * `secure-vault`'s `AndroidManifest.xml`. The Startup runtime runs every
 * registered initializer **before `Application.onCreate()` returns**, which
 * means [SecureVaultStartup.applicationContext] is set before any call site
 * could reasonably reach it.
 *
 * Consumers that intentionally disable Startup (e.g. by removing the
 * `<provider>` from their manifest) must call
 * `SecureVault.initialize(context)` from `Application.onCreate()` themselves.
 *
 * Visibility is `public` because the class name must be reflectively loadable
 * by the Startup runtime; the companion holder is `internal`.
 */
public class SecureVaultStartup : Initializer<Unit> {

    override fun create(context: Context) {
        applicationContext = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    public companion object {

        @Volatile
        internal var applicationContext: Context? = null
            private set

        /** Idempotent. Safe to call from any thread. */
        internal fun ensureInitialized(context: Context) {
            if (applicationContext == null) {
                synchronized(this) {
                    if (applicationContext == null) {
                        applicationContext = context.applicationContext
                    }
                }
            }
        }

        internal fun requireContext(): Context = applicationContext
            ?: error(
                "SecureVault has not been initialised. Either keep the default " +
                    "androidx.startup integration enabled (the library ships an " +
                    "Initializer that runs automatically), or call " +
                    "SecureVault.initialize(applicationContext) once from " +
                    "Application.onCreate().",
            )
    }
}

