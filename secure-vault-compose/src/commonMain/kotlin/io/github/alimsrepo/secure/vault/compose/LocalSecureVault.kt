/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import io.github.alimsrepo.secure.vault.SecureVault

/**
 * `CompositionLocal` that exposes the currently-installed [SecureVault] to any
 * composable in the tree below a [ProvideSecureVault] call.
 *
 * Reading this outside a [ProvideSecureVault] scope is a programmer error and
 * throws with a clear message. There is intentionally **no** default value —
 * silently constructing a vault on demand would hide initialisation cost from
 * the caller.
 *
 * ```kotlin
 * @Composable
 * fun LoginScreen() {
 *     val vault = LocalSecureVault.current
 *     val scope = rememberCoroutineScope()
 *     Button(onClick = { scope.launch { vault.put("token", "...") } }) {
 *         Text("Sign in")
 *     }
 * }
 * ```
 *
 * @since 0.1.0
 */
public val LocalSecureVault: ProvidableCompositionLocal<SecureVault> =
    compositionLocalOf {
        error(
            "LocalSecureVault has no value. Wrap your tree in " +
                "ProvideSecureVault(vault) { ... } — typically inside the " +
                "VaultState.Ready branch of rememberSecureVault(...).",
        )
    }

/**
 * Provides [vault] to the composition rooted at [content] via [LocalSecureVault].
 *
 * Pair with [rememberSecureVault]:
 *
 * ```kotlin
 * val state by rememberSecureVault("com.acme.auth")
 * when (val s = state) {
 *     is VaultState.Ready -> ProvideSecureVault(s.vault) { HomeScreen() }
 *     // ...
 * }
 * ```
 *
 * @since 0.1.0
 */
@Composable
public fun ProvideSecureVault(
    vault: SecureVault,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSecureVault provides vault, content = content)
}

