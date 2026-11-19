package app.meows.desktop.ui.home

import app.meows.desktop.i18n.tr
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import app.meows.desktop.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Режимы простыми словами: коротко, без терминов. */
@Composable
fun ModeInfoDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(tr("Какой режим выбрать?")) },
        text = {
            Column {
                Text(tr("Все программы"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(tr("VPN на весь компьютер, как на телефоне: браузеры, игры, мессенджеры — всё идёт через Meows по правилам. Нужны права администратора, Meows попросит их при запуске."))
                Spacer(Modifier.height(16.dp))
                Text(tr("Системный прокси"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(tr("Meows лишь просит Windows направить трафик через VPN. Браузеры слушаются, а игры и многие программы — нет, они идут напрямую. Администратор не нужен."))
                Spacer(Modifier.height(16.dp))
                Text(tr("Не уверены — оставьте «Все программы»."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(tr("Понятно")) } },
    )
}
