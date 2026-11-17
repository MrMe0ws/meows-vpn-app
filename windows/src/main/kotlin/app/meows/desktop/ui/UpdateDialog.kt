package app.meows.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppInfo
import app.meows.desktop.AppModel
import app.meows.desktop.update.UpdateState

/** «Доступна версия»: две кнопки — «Позже» и главная «Скачать» → «Установить» (как на Android). */
@Composable
fun UpdateDialog(app: AppModel) {
    val u = app.updates
    if (!u.dialogVisible) return
    val s = u.state
    val info = when (s) { is UpdateState.Available -> s.info; is UpdateState.Downloading -> s.info; is UpdateState.Downloaded -> s.info; else -> return }
    val busy = s is UpdateState.Downloading
    AlertDialog(
        onDismissRequest = { if (!busy) u.dismissDialog() },
        title = { Text("Доступна версия ${info.version}") },
        text = {
            Column {
                Text("Сейчас установлена ${AppInfo.VERSION} · ${formatBytesRu(info.size)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()).padding(14.dp)) { Text(info.notes.trim()) }
                    }
                }
                if (s is UpdateState.Downloading) {
                    Spacer(Modifier.height(16.dp)); Text("Скачиваем… ${s.percent}%"); Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            when (s) {
                is UpdateState.Downloaded -> Button(onClick = { app.installUpdate() }) { Text("Установить") }
                is UpdateState.Available -> Button(onClick = u::download) { Text("Скачать") }
                else -> {}
            }
        },
        dismissButton = { if (!busy) TextButton(onClick = u::dismissDialog) { Text("Позже") } },
    )
}