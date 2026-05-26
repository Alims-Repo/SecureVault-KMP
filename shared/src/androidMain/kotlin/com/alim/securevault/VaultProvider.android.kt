package com.alim.securevault

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.alimsrepo.secure.vault.SecureVaultFactory
import io.github.alimsrepo.secure.vault.VaultConfig

/**
 * Process-wide singleton keyed on `applicationContext`. Double-checked locking
 * keeps construction cheap on every recomposition while guaranteeing the
 * Keystore handshake runs exactly once.
 */
private object AndroidVaultHostHolder {
    @Volatile private var host: VaultHost? = null

    fun get(appContext: Context): VaultHost =
        host ?: synchronized(this) {
            host ?: VaultHost {
                SecureVaultFactory(appContext).create(
                    VaultConfig(namespace = "com.alim.securevault.sample"),
                )
            }.also { host = it }
        }
}

@Composable
internal actual fun rememberVaultState(): State<VaultState> {
    val appContext = LocalContext.current.applicationContext
    val host = remember(appContext) { AndroidVaultHostHolder.get(appContext) }
    return host.state.collectAsState()
}

