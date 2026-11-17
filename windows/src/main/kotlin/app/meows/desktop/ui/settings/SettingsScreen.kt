package app.meows.desktop.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppInfo
import app.meows.desktop.AppModel
import app.meows.desktop.ui.home.openUrl
import app.meows.desktop.ui.theme.Palettes

private const val GITHUB = "https://github.com/MrMe0ws/meows-vpn-app"

@Composable
internal fun Group(title: String, footer: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
        Text(title.uppercase(), Modifier.padding(start = 12.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
        if (footer != null) Text(footer, Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) = ListItem(
    modifier = Modifier.clickable { onChange(!checked) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    headlineContent = { Text(title) }, supportingContent = sub?.let { { Text(it) } }, trailingContent = { Switch(checked, onChange) },
)

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) = Surface(
    onClick = onClick, shape = CircleShape, color = color, modifier = Modifier.size(44.dp),
    border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
) { Box(contentAlignment = Alignment.Center) { if (selected) Icon(Icons.Rounded.Check, "Выбрано", tint = Color.Black.copy(alpha = 0.75f)) } }

/** Настройки: тема и палитра (как на Android), маршрутизация, о приложении. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: AppModel, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { TopAppBar(title = { Text("Настройки") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } }) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            Group("Тема") {
                val modes = listOf("Авто", "Светлая", "Тёмная")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                    modes.forEachIndexed { i, m -> SegmentedButton(selected = app.themeMode == i, onClick = { app.setTheme(i) }, shape = SegmentedButtonDefaults.itemShape(i, modes.size)) { Text(m) } }
                }
            }
            Group("Цвет", "«Серая» и «Монохром» — спокойные палитры без яркого цвета.") {
                Column(Modifier.padding(16.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Palettes.seeds.forEachIndexed { i, c -> Swatch(c, app.palette == i) { app.choosePalette(i) } }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(Palettes.names[app.palette.coerceIn(0, Palettes.names.lastIndex)], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SwitchRow("Чёрный фон (AMOLED)", "Для тёмной темы", app.amoled, app::chooseAmoled)
            }
            Group("Маршрутизация", "Основные правила (российское напрямую, заблокированное через VPN) приходят вместе с подпиской.") {
                SwitchRow("Блокировать рекламу", null, app.blockAds, app::chooseBlockAds)
                SwitchRow("Все российские сайты напрямую", "Расширенно, поверх правил подписки", app.ruDirect, app::chooseRuDirect)
            }
            SystemGroup(app)
            UpdatesGroup(app)
            Group("О приложении") {
                ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), headlineContent = { Text("Версия") }, trailingContent = { Text(AppInfo.VERSION) })
                ListItem(modifier = Modifier.clickable { openUrl(GITHUB) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent), headlineContent = { Text("Проект на GitHub") }, supportingContent = { Text("github.com/MrMe0ws/meows-vpn-app") })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}