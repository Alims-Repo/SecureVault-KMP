/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import io.github.alimsrepo.secure.vault.SecureVault
import io.github.alimsrepo.secure.vault.VaultException
import kotlinx.coroutines.flow.drop

/**
 * Two-way binding between a Compose [MutableState] and a single [SecureVault] key.
 *
 * Reads come from [SecureVault.observe] — the state stays in sync with writes
 * from any code path. Writes assigned through the returned state propagate
 * back to the vault on a background dispatcher. The vault itself comes from
 * [LocalSecureVault], so callers do not need to plumb it manually.
 *
 * ```kotlin
 * @Composable
 * fun LoginScreen() {
 *     var token by rememberSecureValue("auth.token", default = "")
 *     OutlinedTextField(value = token, onValueChange = { token = it })
 * }
 * ```
 *
 * Error policy: failures inside [SecureVault.put] / [SecureVault.observe] are
 * swallowed silently to keep the UI from crashing during normal use; the last
 * known good value is retained. Callers that need to surface errors should
 * use [SecureVault.observe] and [SecureVault.put] directly.
 *
 * @param key non-blank key inside the current vault's namespace.
 * @param default the value to expose until the first emission arrives (and to
 *                fall back to whenever the stored value is `null`).
 * @return a [MutableState] whose reads are reactive and whose writes persist.
 *
 * @since 0.3.0
 */
@Composable
public fun rememberSecureValue(
    key: String,
    default: String = "",
): MutableState<String> {
    val vault = LocalSecureVault.current
    return rememberSecureValue(vault, key, default)
}

/**
 * Overload that accepts an explicit [vault] instead of resolving it from
 * [LocalSecureVault]. Useful for tests and for screens that bridge multiple
 * namespaces.
 *
 * @since 0.3.0
 */
@Composable
public fun rememberSecureValue(
    vault: SecureVault,
    key: String,
    default: String = "",
): MutableState<String> {
    val state = remember(vault, key) { mutableStateOf(default) }

    // Vault -> state. A new observation starts whenever vault or key changes.
    LaunchedEffect(vault, key) {
        try {
            vault.observe(key).collect { value ->
                state.value = value ?: default
            }
        } catch (_: VaultException) {
            // Swallow — see KDoc. State retains the last good value.
        }
    }

    // State -> vault. drop(1) skips the initial value we just set so we don't
    // round-trip the default back into storage on first composition.
    LaunchedEffect(vault, key) {
        snapshotFlow { state.value }
            .drop(1)
            .collect { value ->
                try {
                    vault.put(key, value)
                } catch (_: VaultException) {
                    // Swallow — see KDoc.
                }
            }
    }

    return state
}

