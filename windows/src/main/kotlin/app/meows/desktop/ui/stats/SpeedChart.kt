package app.meows.desktop.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.meows.desktop.stats.StatsFeed

private const val POINTS = 120   // две минуты по точке в секунду

/** График скорости: получение (цвет темы) и отправка (дополнительный цвет); шкала подстраивается под максимум, не меньше 10 КБ/с. */
@Composable
fun SpeedChart(samples: List<StatsFeed.Sample>, modifier: Modifier = Modifier) {
    val down = MaterialTheme.colorScheme.primary
    val up = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val max = (samples.maxOfOrNull { maxOf(it.down, it.up) } ?: 0L).coerceAtLeast(10_000L).toFloat()
        for (i in 1..3) { val y = size.height * i / 4; drawLine(grid.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx()) }
        val step = size.width / (POINTS - 1)
        val offset = POINTS - samples.size   // график «приезжает» справа
        fun line(sel: (StatsFeed.Sample) -> Long): Path {
            val p = Path()
            samples.forEachIndexed { i, s ->
                val x = (offset + i) * step; val y = size.height - (sel(s) / max) * (size.height - 4.dp.toPx()) - 2.dp.toPx()
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            return p
        }
        if (samples.size >= 2) {
            val d = line { it.down }
            val fill = Path().apply { addPath(d); lineTo(size.width, size.height); lineTo(offset * step, size.height); close() }
            drawPath(fill, Brush.verticalGradient(listOf(down.copy(alpha = 0.35f), Color.Transparent)))
            drawPath(d, down, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            drawPath(line { it.up }, up, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
