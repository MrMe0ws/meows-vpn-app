package app.meows.vpn.ui.settings

import app.meows.vpn.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel

/** Состояние прокрутки текущей страницы: хранится в [SettingsScreen], чтобы переживать уход на подстраницу и возврат. */
val LocalPageScroll = compositionLocalOf<ScrollState?> { null }

/** Каркас страницы настроек: большой заголовок со сворачиванием и кнопка «назад». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageScaffold(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Назад")) } },
                actions = actions,
                scrollBehavior = scroll,
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(LocalPageScroll.current ?: rememberScrollState())) {
            content()
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

/**
 * Группа настроек, как в системных настройках Android: каждый пункт — своя плитка, между плитками крошечный зазор.
 * Пункты одного смысла кладём в одну группу, разные по смыслу — в разные (между группами зазор больше).
 * Внешние углы группы крупные, внутренние — мелкие. Плиткой становится каждый верхний элемент [content].
 */
@Composable
fun SettingsGroup(title: String? = null, footer: String? = null, content: @Composable () -> Unit) {
    val tile = MaterialTheme.colorScheme.surfaceContainer
    Column(Modifier.padding(horizontal = 16.dp).padding(top = GROUP_GAP)) {
        if (title != null) Text(
            title.uppercase(), Modifier.padding(start = 12.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        )
        val tiles = remember { ArrayList<Rect>() }
        Layout(
            content,
            Modifier.fillMaxWidth().drawBehind {
                tiles.forEachIndexed { i, r ->
                    val top = if (i == 0) TILE_OUTER else TILE_INNER
                    val bottom = if (i == tiles.lastIndex) TILE_OUTER else TILE_INNER
                    val t = CornerRadius(top.toPx()); val b = CornerRadius(bottom.toPx())
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
                    val shape = RoundedCornerShape(
                        if (i == 0) TILE_OUTER else TILE_INNER, if (i == 0) TILE_OUTER else TILE_INNER,
                        if (i == shown.lastIndex) TILE_OUTER else TILE_INNER, if (i == shown.lastIndex) TILE_OUTER else TILE_INNER,
                    )
                    tiles.add(Rect(0f, y.toFloat(), constraints.maxWidth.toFloat(), (y + p.height).toFloat()))
                    p.placeWithLayer(0, y) { this.shape = shape; clip = true }
                    y += p.height + gap
                }
            }
        }
        if (footer != null) Text(
            footer, Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val TILE_OUTER = 28.dp
private val TILE_INNER = 4.dp
private val TILE_GAP = 2.dp
private val GROUP_GAP = 20.dp   // между группами по смыслу — заметно больше, чем зазор между плитками внутри группы

@Composable
fun RowIcon(icon: ImageVector) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = CircleShape) {
        Icon(icon, null, Modifier.padding(8.dp).size(20.dp))
    }
}

@Composable
fun SwitchRow(icon: ImageVector, title: String, checked: Boolean, subtitle: String? = null, enabled: Boolean = true, busy: Boolean = false, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = when {
            busy -> Modifier
            enabled -> Modifier.clickable { onChange(!checked) }
            else -> Modifier.alpha(0.5f)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { RowIcon(icon) },
        headlineContent = { Text(title) },
        // busy — настройка применяется переподключением VPN: человек остаётся на этой странице и должен видеть, что идёт работа
        supportingContent = if (busy) { { Text(tr("Применяем настройку…"), color = MaterialTheme.colorScheme.primary) } } else subtitle?.let { { Text(it) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)) }
                Switch(checked, onChange, enabled = enabled && !busy)
            }
        },
    )
}

/** Строка с переходом и отдельным тумблером (как «Wi‑Fi» в настройках Android): текст открывает подробности, тумблер включает. */
@Composable
fun SplitSwitchRow(icon: ImageVector, title: String, checked: Boolean, subtitle: String?, onOpen: () -> Unit, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.height(IntrinsicSize.Min).alpha(if (enabled) 1f else 0.5f)) {
        // текст и стрелка — одна нажимаемая область: нажатие подсвечивает их вместе
        Row(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            ListItem(
                modifier = Modifier.weight(1f),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { RowIcon(icon) },
                headlineContent = { Text(title) },
                supportingContent = subtitle?.let { { Text(it) } },
            )
            Icon(Icons.Rounded.ChevronRight, null, Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        VerticalDivider(Modifier.padding(vertical = 12.dp))
        Box(Modifier.fillMaxHeight().clickable(enabled = enabled) { onChange(!checked) }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Switch(checked, onChange, enabled = enabled)
        }
    }
}

/** Строка «название — значение». Со стрелкой, если есть переход ([onClick]). */
@Composable
fun ValueRow(icon: ImageVector, title: String, value: String? = null, summary: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ListItem(
            modifier = Modifier.weight(1f),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { RowIcon(icon) },
            headlineContent = { Text(title) },
            supportingContent = summary?.let { { Text(it) } },
        )
        // справа — вне ListItem: внутри него значок прижимается к верху, когда подпись в несколько строк
        if (value != null) Text(value, Modifier.padding(end = if (onClick != null) 0.dp else 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        if (onClick != null) Icon(Icons.Rounded.ChevronRight, null, Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Строка с выбором одного значения из списка в диалоге; значение хранится в prefs по [key]. */
@Composable
fun PickRow(
    vm: MeowsViewModel, icon: ImageVector, title: String, key: String, def: Int,
    names: List<String>, values: List<Int>, reconnect: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    val cur = vm.pref(key, def)
    ValueRow(icon, title, names.getOrNull(values.indexOf(cur)) ?: names.first()) { open = true }
    if (open) ChoiceDialog(title, names, values.indexOf(cur), onDismiss = { open = false }) {
        vm.setPref(key, values[it], reconnect); open = false
    }
}

@Composable
fun ChoiceDialog(title: String, names: List<String>, selected: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                names.forEachIndexed { i, n ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(i) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(i == selected, { onPick(i) })
                        Text(n, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена")) } },
    )
}
