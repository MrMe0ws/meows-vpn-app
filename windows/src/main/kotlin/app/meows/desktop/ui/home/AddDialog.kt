package app.meows.desktop.ui.home

import app.meows.desktop.i18n.tr
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.AlertDialog
import app.meows.desktop.ui.components.Button
import androidx.compose.material3.Icon
import app.meows.desktop.ui.components.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import app.meows.desktop.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager

/** Ввод ссылки подписки (можно вставить из буфера обмена; поддерживаются ссылки вида meows://add/…). */
@Composable
fun AddDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    val clip = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Ссылка подписки")) },
        text = {
            OutlinedTextField(
                url, { url = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("https://…") },
                trailingIcon = { IconButton(onClick = { url = clip.getText()?.text?.trim().orEmpty() }) { Icon(Icons.Rounded.ContentPaste, tr("Вставить")) } },
            )
        },
        confirmButton = { Button(onClick = { onAdd(url); onDismiss() }, enabled = url.isNotBlank()) { Text(tr("Добавить")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена")) } },
    )
}