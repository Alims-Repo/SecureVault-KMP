/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault.compose

import io.github.alimsrepo.secure.vault.SecureVault

/**
 * Lifecycle of a process-wide [SecureVault], surfaced as state your Compose
 * UI is expected to render explicitly.
 *
 * Why a state machine? Both Android (Keystore handshake) and iOS (Keychain
 * cold start) can do tens to thousands of milliseconds of work the first time
 * a vault is touched, and *can* fail terminally (e.g. Keystore wipe after a
 * factory reset). Surfacing those as distinct UI states — rather than hiding
 * them behind an optimistic "always Ready" view — is the difference between
 * a polished app and one that races against itself on cold start.
 *
 * Get one via [rememberSecureVault]; render via:
 *
 * ```kotlin
 * when (val s = state) {
 *     VaultState.Initializing -> SplashScreen()
 *     is VaultState.Failed    -> ErrorScreen(s.reason)
 *     is VaultState.Ready     -> ProvideSecureVault(s.vault) { HomeScreen() }
 * }
 * ```
 *
 * @since 0.1.0
 */
public sealed interface VaultState {

    /** Pre-warm in progress on a background dispatcher. Show a splash. */
    public data object Initializing : VaultState

    /** Vault is usable. Stable: a Ready instance is never reset back to Initializing. */
    public data class Ready(public val vault: SecureVault) : VaultState

    /**
     * Backend rejected the handshake (e.g. Keystore wipe). Terminal for this
     * process — `reason` is a short, log-friendly string. Recovering usually
     * means reinstalling the app or clearing its data.
     */
    public data class Failed(public val reason: String) : VaultState
}

