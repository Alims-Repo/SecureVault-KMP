/*
 * Copyright 2026 Alim Sourav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.alimsrepo.secure.vault.compose.internal

import io.github.alimsrepo.secure.vault.SecureVault
import io.github.alimsrepo.secure.vault.VaultConfig
import io.github.alimsrepo.secure.vault.compose.VaultState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-wide owner of a single [SecureVault] for a given namespace. Triggers
 * the platform's master-key handshake on a background dispatcher inside its
 * constructor's coroutine so the first *user* interaction is never the one
 * that pays the cost.
 *
 * Not part of the public API. Held by [VaultHostRegistry] and exposed through
 * `rememberSecureVault`.
 */
internal class VaultHost(build: () -> SecureVault) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<VaultState>(VaultState.Initializing)

    val state: StateFlow<VaultState> = _state.asStateFlow()

    init {
        scope.launch {
            _state.value = try {
                val vault = build()
                // Warm-up: triggers the master-key derivation / Keychain handshake
                // now, not on the first user tap. keys() is the cheapest read.
                vault.keys()
                VaultState.Ready(vault)
            } catch (t: Throwable) {
                VaultState.Failed(t.message ?: t::class.simpleName ?: "unknown error")
            }
        }
    }
}

/**
 * One [VaultHost] per [VaultConfig.namespace]. Multiple `rememberSecureVault`
 * calls with the same namespace share the same host (and therefore the same
 * pre-warm, the same Ready instance) for the life of the process.
 */
internal object VaultHostRegistry {

    private val hosts = mutableMapOf<String, VaultHost>()
    private val lock = Any()

    fun acquire(config: VaultConfig, build: () -> SecureVault): VaultHost {
        // Validate config (will throw `IllegalArgumentException` on invalid namespace).
        val key = config.namespace
        synchronized(lock) {
            hosts[key]?.let { return it }
            val host = VaultHost(build)
            hosts[key] = host
            return host
        }
    }
}

