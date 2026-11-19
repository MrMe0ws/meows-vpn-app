package app.meows.vpn.ui.home

import app.meows.vpn.i18n.trf
import app.meows.vpn.i18n.tr
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.meows.vpn.Edition
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.components.formatBytesRu
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.fillMaxWidth as fillW

private val RU = Locale.forLanguageTag("ru")

/** Карточка подписки: название, обновить, пинг, меню (…), трафик, срок, объявление. Верх списка серверов. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionHeader(vm: MeowsViewModel, st: XKStore, expanded: Boolean, onExpand: () -> Unit, onReplace: () -> Unit, onSupport: () -> Unit, modifier: Modifier = Modifier) {
    vm.rev
    val total = st.total(); val used = st.used(); val expire = st.expire() * 1000L
    val ctx = LocalContext.current
    val shape = if (expanded) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) else RoundedCornerShape(28.dp)
    val chevron by animateFloatAsState(if (expanded) 0f else -90f, label = "chevron")
    var menu by remember { mutableStateOf(false) }

    Surface(modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExpand) { Icon(Icons.Rounded.ExpandMore, tr("Свернуть список"), Modifier.rotate(chevron)) }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(st.title(), Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (st.isPinned()) Icon(Icons.Rounded.PushPin, tr("Закреплена"), Modifier.padding(start = 6.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val upd = st.updated()
                    val auto = st.autoUpdateHours()
                    Text(
                        (if (upd > 0) SimpleDateFormat("dd.MM.yyyy HH:mm", RU).format(Date(upd)) else "—") +
                            if (auto > 0) trf(" · автообновление %d ч", auto) else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                    )
                }
                val inf = rememberInfiniteTransition(label = "refresh")
                val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "rot")
                IconButton(onClick = { vm.refresh(st, false) }, enabled = !vm.isRefreshing(st)) {
                    Icon(Icons.Rounded.Refresh, tr("Обновить подписку"), Modifier.rotate(if (vm.isRefreshing(st)) rot else 0f))
                }
                IconButton(onClick = { vm.ping(st) }, enabled = !vm.isPinging(st)) {
                    if (vm.isPinging(st)) LoadingIndicator(Modifier.size(24.dp)) else Icon(Icons.Rounded.Speed, tr("Пинг серверов подписки"))
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, tr("Меню подписки")) }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (st.isPinned()) tr("Открепить") else tr("Закрепить сверху")) }, leadingIcon = { Icon(Icons.Rounded.PushPin, null) },
                            onClick = { menu = false; vm.togglePin(st) },
                        )
                        DropdownMenuItem(
                            text = { Text(tr("Выбрать быстрейший")) }, leadingIcon = { Icon(Icons.Rounded.Bolt, null) },
                            onClick = { menu = false; vm.selectFastest(st) },
                        )
                        DropdownMenuItem(
                            text = { Text(tr("Скопировать ссылку")) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                            onClick = { menu = false; vm.copy(st.url()); vm.toast = tr("Ссылка скопирована") },
                        )
                        DropdownMenuItem(
                            text = { Text(tr("Заменить ссылку")) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { menu = false; onReplace() },
                        )
                        val web = st.webUrl()
                        if (!Edition.isPlay && !web.isNullOrBlank()) DropdownMenuItem(
                            text = { Text(tr("Личный кабинет")) }, leadingIcon = { Icon(Icons.Rounded.OpenInBrowser, null) },
                            onClick = { menu = false; openUrl(ctx, web) },
                        )
                        DropdownMenuItem(
                            text = { Text(tr("Удалить подписку"), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; vm.deleteSubscription(st) },
                        )
                    }
                }
            }

            Row(
                Modifier.fillW().background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val frac = if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f
                Box(
                    Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.align(Alignment.CenterStart).fillW(frac).fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
                    Text(
                        formatBytesRu(used) + " / " + (if (total > 0) formatBytesRu(total) else "∞"),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (expire > 0) {
                    Spacer(Modifier.width(12.dp))
                    val low = expire < System.currentTimeMillis() + 3 * 86_400_000L
                    Text(
                        tr("Истекает: ") + SimpleDateFormat("dd.MM.yyyy", RU).format(Date(expire)),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            val ann = st.announce()
            if (!Edition.isPlay && !ann.isNullOrBlank()) {
                Text(
                    ann, Modifier.fillW().background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                )
            }
            AccountActions(vm, st, onSupport)
        }
    }
}

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
