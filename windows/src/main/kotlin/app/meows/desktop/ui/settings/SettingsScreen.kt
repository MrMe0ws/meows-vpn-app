package app.meows.desktop.ui.settings

import app.meows.desktop.i18n.Lang
import app.meows.desktop.i18n.tr
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import app.meows.desktop.ui.components.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import app.meows.desktop.ui.components.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import app.meows.desktop.ui.components.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppInfo
import app.meows.desktop.AppModel
import app.meows.desktop.ui.components.hand
import app.meows.desktop.ui.home.openUrl
import app.meows.desktop.ui.theme.Palettes

/**
 * Группа настроек, как в системных настройках Android: каждый пункт — своя плитка, между плитками крошечный зазор.
 * Внешние углы группы крупные, внутренние — мелкие. Плиткой становится каждый верхний элемент [content].
 */
@Composable
internal fun Group(title: String, footer: String? = null, content: @Composable () -> Unit) {
    val tile = MaterialTheme.colorScheme.surfaceContainer
    Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
        Text(title.uppercase(), Modifier.padding(start = 12.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        val tiles = remember { ArrayList<Rect>() }
        Layout(
            content,
            Modifier.fillMaxWidth().drawBehind {
                tiles.forEachIndexed { i, r ->
                    val t = CornerRadius((if (i == 0) TILE_OUTER else TILE_INNER).toPx())
                    val b = CornerRadius((if (i == tiles.lastIndex) TILE_OUTER else TILE_INNER).toPx())
                    drawPath(Path().apply { addRoundRect(RoundRect(r, t, t, b, b)) }, tile)
                }
            },
        ) { measurables, constraints ->
            val gap = TILE_GAP.roundToPx()
            val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            val shown = placeables.filter { it.height > 0 }   // пустые узлы (например, диалоги) плитками не считаем
            val height = shown.sumOf { it.height } + gap * (shown.size - 1).coerceAtLeast(0)
            layout(constraints.maxWidth, height) {
                tiles.clear()
                var y = 0
                placeables.forEach { p ->
                    if (p.height == 0) { p.place(0, y); return@forEach }
                    val i = tiles.size
                    val top = if (i == 0) TILE_OUTER else TILE_INNER
                    val bottom = if (i == shown.lastIndex) TILE_OUTER else TILE_INNER
                    val shape = RoundedCornerShape(top, top, bottom, bottom)
                    tiles.add(Rect(0f, y.toFloat(), constraints.maxWidth.toFloat(), (y + p.height).toFloat()))
                    p.placeWithLayer(0, y) { this.shape = shape; clip = true }
                    y += p.height + gap
                }
            }
        }
        if (footer != null) Text(footer, Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val TILE_OUTER = 28.dp
private val TILE_INNER = 4.dp
private val TILE_GAP = 2.dp

@Composable
internal fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) = SwitchRow(title, sub, checked, true, onChange)

@Composable
internal fun SwitchRow(title: String, sub: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) = ListItem(
    modifier = if (enabled) Modifier.clickable { onChange(!checked) }.hand() else Modifier.alpha(0.5f), colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    headlineContent = { Text(title) }, supportingContent = sub?.let { { Text(it) } }, trailingContent = { Switch(checked, onChange, enabled = enabled) },
)

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) = Surface(
    onClick = onClick, shape = CircleShape, color = color, modifier = Modifier.size(44.dp).hand(),
    border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
) { Box(contentAlignment = Alignment.Center) { if (selected) Icon(Icons.Rounded.Check, tr("Выбрано"), tint = Color.Black.copy(alpha = 0.75f)) } }

/** Настройки: тема и палитра (как на Android), маршрутизация, о приложении. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: AppModel, onBack: () -> Unit) {
    var autoSwitchOpen by remember { mutableStateOf(false) }
    var profilesOpen by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf<String?>(null) }   // id профиля маршрутизации в редакторе
    val scroll = rememberScrollState()   // создаётся до раннего return: иначе уход в подстраницу сбрасывает прокрутку
    if (autoSwitchOpen) { AutoSwitchScreen(app) { autoSwitchOpen = false }; return }
    editProfile?.let { ProfileEditScreen(app, it) { editProfile = null }; return }
    if (profilesOpen) { ProfilesScreen(app, { profilesOpen = false }) { editProfile = it }; return }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { TopAppBar(title = { Text(tr("Настройки")) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Назад")) } }) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(scroll)) {
            Group(tr("Язык")) {
                val langs = listOf(tr("Авто"), "Русский", "English")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                    langs.forEachIndexed { i, m -> SegmentedButton(selected = Lang.choice == i, onClick = { app.chooseLang(i) }, shape = SegmentedButtonDefaults.itemShape(i, langs.size)) { Text(m) } }
                }
            }
            Group(tr("Тема")) {
                val modes = listOf(tr("Авто"), tr("Светлая"), tr("Тёмная"))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                    modes.forEachIndexed { i, m -> SegmentedButton(selected = app.themeMode == i, onClick = { app.setTheme(i) }, shape = SegmentedButtonDefaults.itemShape(i, modes.size)) { Text(m) } }
                }
            }
            Group(tr("Цвет"), tr("«Серая» и «Монохром» — спокойные палитры без яркого цвета.")) {
                Column(Modifier.padding(16.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Palettes.seeds.forEachIndexed { i, c -> Swatch(c, app.palette == i) { app.choosePalette(i) } }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(Palettes.names[app.palette.coerceIn(0, Palettes.names.lastIndex)], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SwitchRow(tr("Чёрный фон (AMOLED)"), tr("Для тёмной темы"), app.amoled, app::chooseAmoled)
            }
            Group(tr("Маршрутизация"), tr("Основные правила (российское напрямую, заблокированное через VPN) приходят вместе с подпиской.")) {
                SwitchRow(tr("Весь трафик через VPN"), null, app.allVpn, app::chooseAllVpn)
                SwitchRow(tr("Блокировать рекламу"), null, app.blockAds, app::chooseBlockAds)
                SwitchRow(tr("Все российские сайты напрямую"), if (app.allVpn) tr("В режиме «Весь трафик через VPN» не действует") else tr("Расширенно, поверх правил подписки"), app.ruDirect, !app.allVpn, app::chooseRuDirect)
            }
            Group(tr("Для опытных"), tr("Свои правила маршрутизации. Если не уверены, оставьте по умолчанию.")) {
                NavRow(tr("Профили маршрутизации"), routeProfileTitle(app)) { profilesOpen = true }
            }
            SystemGroup(app) { autoSwitchOpen = true }
            UpdatesGroup(app)
            Group(tr("О приложении")) {
                ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), headlineContent = { Text(tr("Версия")) }, trailingContent = { Text(AppInfo.VERSION) })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}