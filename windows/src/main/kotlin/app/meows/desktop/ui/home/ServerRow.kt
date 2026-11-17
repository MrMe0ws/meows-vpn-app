package app.meows.desktop.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meows.desktop.data.Server
import app.meows.desktop.ui.Res

/** Строка сервера: флаг, название, протокол, пинг; выбранный подсвечен цветом темы. */
@Composable
fun ServerRow(server: Server, selected: Boolean, last: Boolean, ping: Int?, onSelect: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primaryContainer else cs.surfaceContainer, label = "bg")
    val shape = if (last) RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp) else RoundedCornerShape(0.dp)
    Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable(onClick = onSelect), color = bg, shape = shape) {
        Column {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                FlagBadge(server.flag)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(server.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selected) cs.onPrimaryContainer else cs.onSurface)
                    Text(server.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selected) cs.onPrimaryContainer.copy(alpha = 0.8f) else cs.onSurfaceVariant)
                }
                if (ping != null) PingChip(ping)
                if (selected) { Spacer(Modifier.width(8.dp)); Icon(Icons.Rounded.CheckCircle, "Выбран", tint = cs.primary) }
            }
            if (!last) HorizontalDivider(Modifier.padding(start = 60.dp), color = cs.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun PingChip(ms: Int) {
    val cs = MaterialTheme.colorScheme
    val (text, bg, fg) = when {
        ms <= 0 -> Triple("—", cs.errorContainer, cs.onErrorContainer)
        ms < 150 -> Triple("$ms мс", cs.primaryContainer, cs.onPrimaryContainer)
        ms < 400 -> Triple("$ms мс", cs.tertiaryContainer, cs.onTertiaryContainer)
        else -> Triple("$ms мс", cs.errorContainer, cs.onErrorContainer)
    }
    Surface(color = bg, contentColor = fg, shape = CircleShape) { Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge) }
}
/** Windows не умеет рисовать эмодзи-флаги — показываем картинки флагов из Noto (как на Android); нет картинки — плашка с кодом. */
@Composable
fun FlagBadge(flag: String?) {
    val code = flag?.codePoints()?.toArray()?.filter { it in 0x1F1E6..0x1F1FF }?.map { ('a'.code + it - 0x1F1E6).toChar() }?.joinToString("")
    val img = remember(code) { code?.let { runCatching { Res.bitmap("flags/$it.png") }.getOrNull() } }
    if (img != null) {
        Image(img, code, Modifier.width(44.dp).height(30.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
    } else {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.width(44.dp)) {
            Text(code?.uppercase() ?: "VPN", Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.labelLarge, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}