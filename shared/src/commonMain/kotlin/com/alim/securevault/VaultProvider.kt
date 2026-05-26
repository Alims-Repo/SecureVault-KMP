package com.alim.securevault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Returns the current [VaultState] for the process-wide sample vault.
 *
 * The [VaultHost] is owned at the platform layer (Application-scoped on
 * Android, process-global on iOS) so vault construction happens *exactly
 * once* per process — not once per composition.
 */
@Composable
internal expect fun rememberVaultState(): State<VaultState>

