package com.alim.securevault

import androidx.compose.runtime.Composable
import io.github.alimsrepo.secure.vault.SecureVault

/**
 * Composable factory for a [SecureVault] suitable for the sample app.
 *
 * Each platform supplies whatever environment its native backend needs
 * (Android: an `applicationContext` resolved from `LocalContext`; iOS: nothing,
 * the Keychain is process-wide).
 */
@Composable
internal expect fun rememberSampleVault(): SecureVault

