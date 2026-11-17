package app.meows.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import app.meows.desktop.ui.components.Button
import app.meows.desktop.ui.components.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppInfo
import app.meows.desktop.AppModel
import app.meows.desktop.ui.about.UpdateBlock
import app.meows.desktop.ui.formatBytesRu
import app.meows.desktop.update.UpdateState

/** Автопроверка обновлений (галочка), ручная проверка, скачивание и установка. */
@Composable
fun UpdatesGroup(app: AppModel) {
    val u = app.updates
    var auto by remember { mutableStateOf(u.autoCheck) }
    Group("Обновления", "Приложение обновляется через GitHub: скачивает установщик и ставит его сам. Проверка — не чаще раза в 12 часов.") {
        SwitchRow("Искать обновления", "При запуске приложения", auto) { auto = it; u.autoCheck = it }
        UpdateBlock(app)
    }
}