package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Shown instead of an empty list when the stored notes cannot be decrypted on this device. Nothing
 * is overwritten; erasing them is a separate, confirmed choice.
 */
@Composable
fun UnreadableNotesScreen(onWipe: () -> Unit, modifier: Modifier = Modifier) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .safeDrawingPadding()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
            Text(
                "Notlar açılamadı",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Notların bu cihazda şifreli duruyor ama şifre anahtarına ulaşılamıyor. Hiçbir şey silinmedi. " +
                    "Bir yedeğin varsa, verileri sıfırlayıp onu içe aktarabilirsin.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { confirm = true }, shapes = ButtonDefaults.shapes()) { Text("Verileri sıfırla") }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Notlar kalıcı olarak silinsin mi?") },
            text = { Text("Okunamayan notlar silinir, uygulama boş açılır. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onWipe()
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } },
        )
    }
}
