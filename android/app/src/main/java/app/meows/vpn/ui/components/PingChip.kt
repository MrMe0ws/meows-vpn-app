package app.meows.vpn.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Пинг: null — не показывать, -2 — UDP-протокол, <=0 — нет ответа. Цвет по качеству. */
@Composable
fun PingChip(ms: Int?) {
    if (ms == null) return
    val cs = MaterialTheme.colorScheme
    val (text, bg, fg) = when {
        ms == -2 -> Triple("UDP", cs.surfaceContainerHighest, cs.onSurfaceVariant)
        ms <= 0 -> Triple("—", cs.errorContainer, cs.onErrorContainer)
        ms < 150 -> Triple("$ms мс", cs.primaryContainer, cs.onPrimaryContainer)
        ms < 400 -> Triple("$ms мс", cs.tertiaryContainer, cs.onTertiaryContainer)
        else -> Triple("$ms мс", cs.errorContainer, cs.onErrorContainer)
    }
    Surface(color = bg, contentColor = fg, shape = CircleShape) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
    }
}
