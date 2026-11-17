package app.meows.desktop.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppInfo
import app.meows.desktop.AppModel
import app.meows.desktop.ui.formatBytesRu
import app.meows.desktop.update.UpdateState

/** Состояние обновления и кнопки (проверить · скачать · установить). Используется в «О программе» и в настройках. */
@Composable
fun UpdateBlock(app: AppModel) {
    val u = app.updates
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Установлена: ${AppInfo.VERSION}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        when (val s = u.state) {
            UpdateState.Idle -> Text("Проверка ещё не выполнялась.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            UpdateState.Checking -> Text("Проверяем…")
            UpdateState.UpToDate -> Text("У вас последняя версия.", color = MaterialTheme.colorScheme.primary)
            is UpdateState.Available -> {
                Text("Доступна версия ${s.info.version}" + if (s.info.size > 0) " · ${formatBytesRu(s.info.size)}" else "", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp)); Button(onClick = u::download) { Text("Скачать") }
            }
            is UpdateState.Downloading -> {
                Text("Скачиваем ${s.info.version}… ${s.percent}%"); Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
            }
            is UpdateState.Downloaded -> {
                Text("Версия ${s.info.version} скачана. Приложение перезапустится."); Spacer(Modifier.height(8.dp))
                Button(onClick = app::installUpdate) { Text("Установить") }
            }
            is UpdateState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = { u.check(manual = true) }, enabled = u.state !is UpdateState.Checking && u.state !is UpdateState.Downloading) { Text("Проверить обновления") }
    }
}
