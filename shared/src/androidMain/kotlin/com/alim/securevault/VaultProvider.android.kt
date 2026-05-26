package com.alim.securevault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.alimsrepo.secure.vault.SecureVault
import io.github.alimsrepo.secure.vault.SecureVaultFactory
import io.github.alimsrepo.secure.vault.VaultConfig

@Composable
internal actual fun rememberSampleVault(): SecureVault {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        SecureVaultFactory(context).create(
            VaultConfig(namespace = "com.alim.securevault.sample"),
        )
    }
}

