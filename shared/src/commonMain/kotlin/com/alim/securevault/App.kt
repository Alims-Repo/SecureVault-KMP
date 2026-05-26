package com.alim.securevault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.alimsrepo.secure.vault.SecureVault
import io.github.alimsrepo.secure.vault.VaultException
import io.github.alimsrepo.secure.vault.compose.VaultState
import io.github.alimsrepo.secure.vault.compose.rememberSecureVault
import kotlinx.coroutines.launch

/** UI status banner shown above the form after every vault operation. */
private sealed interface Status {
    data object Idle : Status
    data class Info(val text: String) : Status
    data class Error(val text: String) : Status
}

@Composable
@Preview
fun App() {
    val state by rememberSecureVault("com.alim.securevault.sample")
    SecureVaultApp(state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecureVaultApp(state: VaultState) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "🔒  SecureVault Demo",
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                },
                contentWindowInsets = WindowInsets.safeContent,
            ) { padding ->
                when (state) {
                    VaultState.Initializing -> InitializingScreen(padding)
                    is VaultState.Failed -> FailedScreen(state.reason, padding)
                    is VaultState.Ready -> VaultScreen(state.vault, padding)
                }
            }
        }
    }
}

@Composable
private fun InitializingScreen(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(strokeWidth = 3.dp)
            Spacer(Modifier.size(16.dp))
            Text(
                "Unlocking secure storage…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                "First launch derives a Keystore master key.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FailedScreen(reason: String, contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Vault unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    reason,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    "This usually means the Keystore was reset by the OS. " +
                        "Reinstalling the app or clearing storage will recover.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun VaultScreen(vault: SecureVault, contentPadding: PaddingValues) {
    val scope = rememberCoroutineScope()

    var keyInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<Status>(Status.Idle) }
    var busy by remember { mutableStateOf(false) }
    var storedKeys by remember { mutableStateOf<List<String>>(emptyList()) }

    suspend fun refreshKeys() {
        storedKeys = runCatching { vault.keys().sorted() }.getOrElse { emptyList() }
    }

    LaunchedEffect(Unit) { refreshKeys() }

    fun run(label: String, block: suspend () -> String) {
        if (busy) return
        busy = true
        scope.launch {
            status = try {
                Status.Info(block())
            } catch (e: VaultException.InvalidKey) {
                Status.Error("Invalid key: ${e.message}")
            } catch (e: VaultException.Tampered) {
                Status.Error("Tampered data: ${e.message}")
            } catch (e: VaultException.CryptoFailure) {
                Status.Error("Crypto failure: ${e.message}")
            } catch (e: VaultException.StorageUnavailable) {
                Status.Error("Storage unavailable: ${e.message}")
            } catch (t: Throwable) {
                Status.Error("$label failed: ${t.message ?: t::class.simpleName}")
            }
            refreshKeys()
            busy = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeaderCard(busy = busy, status = status)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Key") },
                    placeholder = { Text("e.g. session.token") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    label = { Text("Value") },
                    placeholder = { Text("Anything secret") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = {
                            val k = keyInput
                            val v = valueInput
                            run("put") {
                                vault.put(k, v)
                                "Stored \"$k\" (${v.length} chars)"
                            }
                        },
                        enabled = !busy && keyInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }

                    FilledTonalButton(
                        onClick = {
                            val k = keyInput
                            run("get") {
                                val v = vault.get(k)
                                if (v == null) {
                                    "No value for \"$k\""
                                } else {
                                    valueInput = v
                                    "Loaded \"$k\""
                                }
                            }
                        },
                        enabled = !busy && keyInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Load") }

                    OutlinedButton(
                        onClick = {
                            val k = keyInput
                            run("remove") {
                                val existed = vault.contains(k)
                                vault.remove(k)
                                if (existed) "Removed \"$k\"" else "\"$k\" was not present"
                            }
                        },
                        enabled = !busy && keyInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete") }
                }
            }
        }

        StoredKeysCard(
            keys = storedKeys,
            busy = busy,
            onRefresh = { run("keys") { "Found ${vault.keys().size} key(s)" } },
            onClear = { run("clear") { vault.clear(); "Vault cleared" } },
            onPickKey = { keyInput = it },
        )
    }
}

@Composable
private fun HeaderCard(busy: Boolean, status: Status) {
    val (bg, fg) = when (status) {
        is Status.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = fg,
                )
                Spacer(Modifier.size(12.dp))
            }
            Column {
                Text(
                    text = "namespace = com.alim.securevault.sample",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = fg,
                )
                Text(
                    text = when (status) {
                        Status.Idle -> "Ready. Try saving a secret below."
                        is Status.Info -> status.text
                        is Status.Error -> status.text
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = fg,
                )
            }
        }
    }
}

@Composable
private fun StoredKeysCard(
    keys: List<String>,
    busy: Boolean,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onPickKey: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Stored keys (${keys.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefresh, enabled = !busy) {
                    Text("↻", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(
                    onClick = onClear,
                    enabled = !busy && keys.isNotEmpty(),
                ) {
                    Text(
                        "✕",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.size(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (keys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No secrets stored yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                ) {
                    items(keys, key = { it }) { k ->
                        KeyRow(key = k, onClick = { onPickKey(k) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyRow(key: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = key,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onClick) { Text("Use") }
    }
}

