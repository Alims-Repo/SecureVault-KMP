package com.alim.securevault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.alimsrepo.secure.vault.SecureVault
import io.github.alimsrepo.secure.vault.SecureVaultFactory
import io.github.alimsrepo.secure.vault.VaultConfig

@Composable
internal actual fun rememberSampleVault(): SecureVault =
    remember {
        SecureVaultFactory().create(
            VaultConfig(namespace = "com.alim.securevault.sample"),
        )
    }

