package app.meows.vpn.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.ui.components.formatBytesRu
import app.meows.vpn.update.UpdateState

/** Диалог «Доступна новая версия»: скачать → установить. Показывается при автопроверке и ручной проверке. */
@Composable
fun UpdateDialog(vm: MeowsViewModel) {
    val u = vm.updates
    if (!u.dialogVisible) return
    val s = u.state
    val info = when (s) {
        is UpdateState.Available -> s.info
        is UpdateState.Downloading -> s.info
        is UpdateState.Downloaded -> s.info
        else -> return
    }

    AlertDialog(
        onDismissRequest = { if (s !is UpdateState.Downloading) u.dismissDialog() },
        title = { Text("Доступна версия ${info.version}") },
        text = {
            Column {
                Text(
                    "Сейчас установлена ${u.currentVersion}" + if (info.apkSize > 0) " · размер ${formatBytesRu(info.apkSize)}" else "",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                        Text(info.notes.trim(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (s is UpdateState.Downloading) {
                    Spacer(Modifier.height(16.dp))
                    Text("Скачиваем… ${s.percent}%", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            when (s) {
                is UpdateState.Available -> TextButton(onClick = u::download) { Text("Скачать") }
                is UpdateState.Downloaded -> TextButton(onClick = {
                    if (u.install()) u.dismissDialog()
                    else vm.toast = "Разрешите установку из этого источника и нажмите «Установить» ещё раз"
                }) { Text("Установить") }
                else -> {}
            }
        },
        dismissButton = {
            if (s !is UpdateState.Downloading) {
                Row2(onSkip = u::skipThisVersion, onLater = u::dismissDialog)
            }
        },
    )
}

@Composable
private fun Row2(onSkip: () -> Unit, onLater: () -> Unit) {
    androidx.compose.foundation.layout.Row {
        TextButton(onClick = onSkip) { Text("Пропустить версию") }
        TextButton(onClick = onLater) { Text("Позже") }
    }
}
