package app.meows.desktop.ui.home

import app.meows.desktop.i18n.trf
import app.meows.desktop.i18n.tr
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import app.meows.desktop.ui.components.DropdownMenuItem
import app.meows.desktop.ui.components.FilledTonalButton
import androidx.compose.material3.Icon
import app.meows.desktop.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.meows.desktop.data.SubscriptionData
import app.meows.desktop.ui.formatBytesRu
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RU = Locale.forLanguageTag("ru")

fun openUrl(url: String) { runCatching { Desktop.getDesktop().browse(URI(url)) } }

fun copyText(text: String) { runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) } }

/**
 * Карточка подписки: название, «Обновить» и «Пинг» этой подписки, меню «⋮» (ссылка, удаление), трафик, срок,
 * объявление, «Кабинет» и «Поддержка». Действия относятся именно к этой подписке — подписок может быть несколько.
 */
@Composable
fun SubscriptionCard(
    sub: SubscriptionData, expanded: Boolean, onExpand: () -> Unit, refreshing: Boolean, pinging: Boolean, pinned: Boolean,
    onRefresh: () -> Unit, onPing: () -> Unit, onFastest: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit,
) {
    val shape = if (expanded) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) else RoundedCornerShape(28.dp)
    val chevron by animateFloatAsState(if (expanded) 0f else -90f, label = "chevron")
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = shape, color = cs.surfaceContainerHigh) {
        Column {
            Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExpand) { Icon(Icons.Rounded.ExpandMore, tr("Свернуть список"), Modifier.rotate(chevron)) }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sub.title, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (pinned) Icon(Icons.Rounded.PushPin, tr("Закреплена"), Modifier.padding(start = 6.dp).size(18.dp), tint = cs.onSurfaceVariant)
                    }
                    Text(
                        SimpleDateFormat("dd.MM.yyyy HH:mm", RU).format(Date(sub.updated)) + if (sub.updateIntervalHours > 0) trf(" · автообновление %d ч", sub.updateIntervalHours) else "",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2,
                    )
                }
                IconButton(onClick = onRefresh, enabled = !refreshing) {
                    if (refreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp) else Icon(Icons.Rounded.Refresh, tr("Обновить подписку"))
                }
                IconButton(onClick = onPing, enabled = !pinging) {
                    if (pinging) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp) else Icon(Icons.Rounded.Speed, tr("Пинг серверов подписки"))
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, tr("Меню подписки")) }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (pinned) tr("Открепить") else tr("Закрепить сверху")) }, leadingIcon = { Icon(Icons.Rounded.PushPin, null) },
                            onClick = { menu = false; onPin() },
                        )
                        DropdownMenuItem(
                            text = { Text(tr("Выбрать быстрейший")) }, leadingIcon = { Icon(Icons.Rounded.Bolt, null) },
                            onClick = { menu = false; onFastest() },
                        )
                        DropdownMenuItem(
                            text = { Text(tr("Скопировать ссылку")) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                            onClick = { menu = false; copyText(sub.url) },
                        )
                        DropdownMenuItem(
                            text = { Text(tr("Удалить подписку"), color = cs.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = cs.error) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().background(cs.surfaceContainerHighest.copy(alpha = 0.5f)).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                val frac = if (sub.total > 0) (sub.used.toFloat() / sub.total).coerceIn(0f, 1f) else 0f
                Box(Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                    Box(Modifier.align(Alignment.CenterStart).fillMaxWidth(frac).fillMaxSize().background(cs.primaryContainer))
                    Text(formatBytesRu(sub.used) + " / " + (if (sub.total > 0) formatBytesRu(sub.total) else "∞"), style = MaterialTheme.typography.labelMedium)
                }
                if (sub.expireSec > 0) {
                    Spacer(Modifier.width(12.dp))
                    val low = sub.expireSec * 1000 < System.currentTimeMillis() + 3 * 86_400_000L
                    Text(
                        tr("Истекает: ") + SimpleDateFormat("dd.MM.yyyy", RU).format(Date(sub.expireSec * 1000)),
                        style = MaterialTheme.typography.labelLarge, color = if (low) cs.error else cs.onSurface,
                    )
                }
            }
            sub.announce?.takeIf { it.isNotBlank() }?.let {
                Text(it, Modifier.fillMaxWidth().background(cs.surfaceContainerHighest.copy(alpha = 0.5f)).padding(horizontal = 16.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { openUrl(sub.webUrl ?: "https://cabinet.me0ws.store") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.AccountCircle, null); Spacer(Modifier.width(8.dp)); Text(tr("Кабинет"))
                }
                FilledTonalButton(onClick = { openUrl(sub.supportUrl ?: "https://t.me/VPNmeows_bot") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.SupportAgent, null); Spacer(Modifier.width(8.dp)); Text(tr("Поддержка"))
                }
            }
        }
    }
}
