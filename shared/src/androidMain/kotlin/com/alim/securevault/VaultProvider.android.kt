package com.alim.securevault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import io.github.alimsrepo.secure.vault.SecureVault

/**
 * Process-wide singleton. No `Context` plumbing — secure-vault 0.2.0 captures
 * the application Context automatically via androidx.startup, so the call
 * site is identical on Android and iOS.
 */
private object AndroidVaultHostHolder {
    @Volatile private var host: VaultHost? = null

    fun get(): VaultHost =
        host ?: synchronized(this) {
            host ?: VaultHost { SecureVault("com.alim.securevault.sample") }
                .also { host = it }
        }
}

@Composable
internal actual fun rememberVaultState(): State<VaultState> {
    val host = remember { AndroidVaultHostHolder.get() }
    return host.state.collectAsState()
}

