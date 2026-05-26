/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import io.github.alimsrepo.secure.vault.Accessibility
import io.github.alimsrepo.secure.vault.SecureVault
import io.github.alimsrepo.secure.vault.VaultConfig
import io.github.alimsrepo.secure.vault.compose.internal.VaultHostRegistry

/**
 * Returns the lifecycle [VaultState] of a process-wide [SecureVault] for the
 * given [config].
 *
 * The first call for a given [VaultConfig.namespace] starts a background
 * pre-warm (Keystore handshake on Android, Keychain probe on iOS). Subsequent
 * calls — from any composable, on any thread, at any point in the process —
 * return the same host, so the warm-up cost is paid exactly once.
 *
 * The returned [State] is collected from a [kotlinx.coroutines.flow.StateFlow];
 * it is safe to use inside `LaunchedEffect`, `derivedStateOf`, etc.
 *
 * ```kotlin
 * @Composable
 * fun AppRoot() {
 *     val state by rememberSecureVault("com.acme.auth")
 *     when (val s = state) {
 *         VaultState.Initializing -> SplashScreen()
 *         is VaultState.Failed    -> ErrorScreen(s.reason)
 *         is VaultState.Ready     -> ProvideSecureVault(s.vault) { HomeScreen() }
 *     }
 * }
 * ```
 *
 * @since 0.1.0
 */
@Composable
public fun rememberSecureVault(config: VaultConfig): State<VaultState> {
    val host = remember(config.namespace) {
        VaultHostRegistry.acquire(config) { SecureVault(config) }
    }
    return host.state.collectAsState()
}

/**
 * Convenience overload — equivalent to
 * `rememberSecureVault(VaultConfig(namespace, accessibility))`.
 *
 * @since 0.1.0
 */
@Composable
public fun rememberSecureVault(
    namespace: String,
    accessibility: Accessibility = Accessibility.AfterFirstUnlock,
): State<VaultState> = rememberSecureVault(
    VaultConfig(namespace = namespace, accessibility = accessibility),
)

