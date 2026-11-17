package app.meows.desktop.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import app.meows.desktop.ui.components.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppInfo
import app.meows.desktop.ui.components.hand
import app.meows.desktop.AppModel
import app.meows.desktop.data.Hwid
import app.meows.desktop.system.Elevation
import app.meows.desktop.ui.home.copyText
import app.meows.desktop.ui.home.openUrl

private const val GITHUB = "https://github.com/MrMe0ws/meows-vpn-app"

@Composable
private fun InfoRow(title: String, value: String, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) = ListItem(
    modifier = if (onClick != null) Modifier.clickable(onClick = onClick).hand() else Modifier,
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    headlineContent = { Text(title) },
    supportingContent = { Text(value, color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
    trailingContent = trailing,
)

/** «О программе»: версия, ядро, режим, идентификатор устройства, ссылки поддержки и проверка обновлений. */
@Composable
fun AboutScreen(app: AppModel) {
    val os = "${System.getProperty("os.name")} ${System.getProperty("os.version")} · ${System.getProperty("os.arch")}"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("О программе", Modifier.padding(start = 8.dp, top = 20.dp, bottom = 16.dp), style = MaterialTheme.typography.headlineMedium)
        Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(vertical = 4.dp)) {
                InfoRow("Версия программы", AppInfo.VERSION)
                InfoRow("Ядро", "Xray-core 26.10.31 (a52db8f)")
                InfoRow("Режим", app.mode.title + if (Elevation.isAdmin()) " · запущено от администратора" else "")
                InfoRow("Устройство", os)
                InfoRow("HWID", Hwid.value) { IconButton(onClick = { copyText(Hwid.value) }) { Icon(Icons.Rounded.ContentCopy, "Скопировать HWID") } }
                InfoRow("Telegram", "@VPNmeows_bot", { openUrl("https://t.me/VPNmeows_bot") })
                InfoRow("Личный кабинет", "cabinet.me0ws.store", { openUrl("https://cabinet.me0ws.store") })
                InfoRow("Проект на GitHub", "github.com/MrMe0ws/meows-vpn-app", { openUrl(GITHUB) })
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { UpdateBlock(app) }
        Spacer(Modifier.height(24.dp))
    }
}
