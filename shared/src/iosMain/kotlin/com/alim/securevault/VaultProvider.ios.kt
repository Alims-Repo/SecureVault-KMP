package com.alim.securevault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import io.github.alimsrepo.secure.vault.SecureVaultFactory
import io.github.alimsrepo.secure.vault.VaultConfig

/** Process-global singleton; the Keychain is itself a process-wide resource. */
private val iosVaultHost: VaultHost by lazy {
    VaultHost {
        SecureVaultFactory().create(
            VaultConfig(namespace = "com.alim.securevault.sample"),
        )
    }
}

@Composable
internal actual fun rememberVaultState(): State<VaultState> =
    iosVaultHost.state.collectAsState()

