package com.alim.securevault

import io.github.alimsrepo.secure.vault.SecureVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One-shot lifecycle states the UI must render explicitly. The Android backend
 * does non-trivial work on the *first* call into the vault (Keystore master-key
 * derivation, file I/O); surfacing that work as a state — rather than hiding
 * it behind an optimistic "Ready" — is the difference between a polished
 * sample and one that races against itself on a cold start.
 */
internal sealed interface VaultState {
    data object Initializing : VaultState
    data class Ready(val vault: SecureVault) : VaultState
    data class Failed(val reason: String) : VaultState
}

/**
 * Owns the [SecureVault] for the whole process and exposes its initialisation
 * lifecycle as a [StateFlow]. Construction is cheap; the actual heavy lifting
 * (Keystore round-trip on Android) runs on [Dispatchers.Default] in the
 * constructor's coroutine, and we deliberately make one warm-up call so the *user's* first interaction
 * does not pay the master-key cost.
 *
 * One instance per process. Do not put it inside `remember`.
 */
internal class VaultHost(build: () -> SecureVault) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<VaultState>(VaultState.Initializing)

    val state: StateFlow<VaultState> = _state.asStateFlow()

    init {
        scope.launch {
            _state.value = try {
                val vault = build()
                // Warm-up: triggers master-key derivation / Keychain handshake
                // *now*, not on the user's first tap.
                vault.keys()
                VaultState.Ready(vault)
            } catch (t: Throwable) {
                VaultState.Failed(t.message ?: t::class.simpleName ?: "unknown error")
            }
        }
    }
}


