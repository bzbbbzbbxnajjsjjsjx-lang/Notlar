package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val MIN_PASSWORD = 8

/** Before exporting: seal the backup with a password, or save it as plain JSON. */
@Composable
fun ExportBackupDialog(onExport: (password: String?) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val password = rememberTextFieldState()
    val repeat = rememberTextFieldState()
    val longEnough = password.text.length >= MIN_PASSWORD
    val same = password.text.toString() == repeat.text.toString()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text("Yedeği şifrele") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Parola verirsen yedek AES-256 ile şifrelenir; açmak için aynı parola gerekir. Parolayı unutursan yedek açılamaz.")
                OutlinedSecureTextField(
                    state = password,
                    label = { Text("Parola (en az $MIN_PASSWORD karakter)") },
                )
                // A typo in a single field would leave a backup nobody can open.
                OutlinedSecureTextField(
                    state = repeat,
                    label = { Text("Parolayı tekrar gir") },
                    isError = repeat.text.isNotEmpty() && !same,
                    supportingText = if (repeat.text.isNotEmpty() && !same) {
                        { Text("Parolalar eşleşmiyor") }
                    } else {
                        null
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onExport(password.text.toString()) }, enabled = longEnough && same) { Text("Şifrele ve aktar") }
        },
        dismissButton = {
            TextButton(onClick = { onExport(null) }) {
                Text("Şifresiz aktar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

/** Importing a sealed backup: ask for its password; [wrong] after a failed try. */
@Composable
fun ImportPasswordDialog(wrong: Boolean, onOpen: (String) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val password = rememberTextFieldState()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text("Şifreli yedek") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (wrong) "Parola yanlış, tekrar dene." else "Bu yedeği açmak için parolasını gir.")
                OutlinedSecureTextField(state = password, label = { Text("Parola") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onOpen(password.text.toString()) }, enabled = password.text.isNotEmpty()) { Text("Aç") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
