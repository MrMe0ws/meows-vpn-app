package app.meows.desktop.ui.settings

import app.meows.desktop.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel
import app.meows.desktop.ui.components.IconButton
import app.meows.desktop.ui.components.SegmentedButton
import app.meows.desktop.ui.components.hand
import app.meows.desktop.ui.home.FlagBadge

/** Автопереключение: тумблер и выбор серверов активной подписки, на которые можно переключаться (все, кроме отмеченных / только отмеченные). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSwitchScreen(app: AppModel, onBack: () -> Unit) {
    val f = app.switchFilter
    val servers = app.sub?.servers.orEmpty()
    val marked = f.marked(servers)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { TopAppBar(title = { Text(tr("Автопереключение")) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Назад")) } }) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            Group(tr("Автопереключение"), tr("Если сервер перестал отвечать, Meows подключится к другому рабочему. Выключите, если нужна конкретная страна.")) {
                SwitchRow(tr("Автопереключение серверов"), null, app.autoSwitch, app::chooseAutoSwitch)
            }
            Group(
                tr("Куда переключаться"),
                if (f.mode == 1) tr("Meows переключится только на отмеченные серверы.")
                else tr("Meows не переключится на отмеченные серверы. Российские исключены по умолчанию: через них не открываются заблокированные сайты. Сервер, выбранный вручную, остаётся как есть."),
            ) {
                val modes = listOf(tr("Все, кроме"), tr("Выбранные"))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                    modes.forEachIndexed { i, m -> SegmentedButton(selected = f.mode == i, onClick = { f.chooseMode(i) }, shape = SegmentedButtonDefaults.itemShape(i, modes.size)) { Text(m) } }
                }
                if (servers.isEmpty()) Text(tr("Добавьте подписку, чтобы выбрать серверы."), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                servers.forEach { s ->
                    val on = s.title in marked
                    ListItem(
                        modifier = Modifier.clickable { f.toggle(servers, s.title) }.hand(), colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { FlagBadge(s.flag) },
                        headlineContent = { Text(s.title) },
                        supportingContent = if (f.mode == 0 && on) { { Text(tr("Исключён")) } } else null,
                        trailingContent = { Checkbox(on, onCheckedChange = null) },
                    )
                }
            }
        }
    }
}
