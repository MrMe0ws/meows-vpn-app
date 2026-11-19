package app.meows.vpn.ui.home

import app.meows.vpn.i18n.tr
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meows.vpn.XKStore
import app.meows.vpn.ui.components.PingChip
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ActionWidth = 96.dp

/**
 * Строка сервера: тап — выбрать, долгое нажатие — конфиг, **свайп справа налево** открывает
 * кнопку «Пинг» (как в Telegram); пинг запускается нажатием на неё. Открытая строка закрывается тапом.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServerRow(
    server: XKStore.Server,
    selected: Boolean,
    last: Boolean,
    ping: Int?,
    pinging: Boolean,
    onSelect: () -> Unit,
    onPing: () -> Unit,
    onConfig: () -> Unit,
) {
    val shape = if (last) RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp) else RoundedCornerShape(0.dp)
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        label = "rowbg",
    )
    val actionPx = with(LocalDensity.current) { ActionWidth.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    fun settle(open: Boolean) {
        scope.launch {
            animate(offset, if (open) -actionPx else 0f, animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) { v, _ -> offset = v }
        }
    }
    val drag = rememberDraggableState { delta -> offset = (offset + delta).coerceIn(-actionPx, 0f) }

    Box(Modifier.padding(horizontal = 12.dp).clip(shape)) {
        // кнопка под строкой
        Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.CenterEnd) {
            Column(
                Modifier.width(ActionWidth).fillMaxHeight().clickable { settle(false); onPing() },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.Speed, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(tr("Пинг"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Surface(
            color = bg,
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .fillMaxWidth()
                .draggable(
                    drag, Orientation.Horizontal,
                    onDragStopped = { velocity -> settle(offset < -actionPx / 2 || velocity < -900f) },
                )
                .combinedClickable(onClick = { if (offset != 0f) settle(false) else onSelect() }, onLongClick = onConfig),
        ) {
            Column {
                Row(Modifier.heightIn(min = 48.dp).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // выбранный сервер — вертикальная линия слева от флага
                    Box(Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent))
                    Spacer(Modifier.width(12.dp))
                    Text(server.flag ?: "🌐", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            server.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            server.subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (pinging) LoadingIndicator(Modifier.size(28.dp)) else PingChip(ping)
                }
                if (!last) HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}
