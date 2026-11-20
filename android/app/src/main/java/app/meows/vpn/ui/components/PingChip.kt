package app.meows.vpn.ui.components

import app.meows.vpn.i18n.tr
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Пинг: null — не показывать, -2 — UDP-протокол, <=0 — нет ответа. Цвет по качеству.
 * [onSelected] — плашка лежит на выбранной (залитой primaryContainer) строке: там контейнерные цвета сливаются с фоном, берём насыщенные.
 */
@Composable
fun PingChip(ms: Int?, onSelected: Boolean = false) {
    if (ms == null) return
    val cs = MaterialTheme.colorScheme
    val (text, bg, fg) = when {
        ms == -2 -> if (onSelected) Triple("UDP", cs.surface, cs.onSurfaceVariant) else Triple("UDP", cs.surfaceContainerHighest, cs.onSurfaceVariant)
        ms <= 0 -> if (onSelected) Triple("—", cs.error, cs.onError) else Triple("—", cs.errorContainer, cs.onErrorContainer)
        ms < 150 -> if (onSelected) Triple("$ms ${tr("мс")}", cs.primary, cs.onPrimary) else Triple("$ms ${tr("мс")}", cs.primaryContainer, cs.onPrimaryContainer)
        ms < 400 -> if (onSelected) Triple("$ms ${tr("мс")}", cs.tertiary, cs.onTertiary) else Triple("$ms ${tr("мс")}", cs.tertiaryContainer, cs.onTertiaryContainer)
        else -> if (onSelected) Triple("$ms ${tr("мс")}", cs.error, cs.onError) else Triple("$ms ${tr("мс")}", cs.errorContainer, cs.onErrorContainer)
    }
    Surface(color = bg, contentColor = fg, shape = CircleShape) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium)
    }
}
