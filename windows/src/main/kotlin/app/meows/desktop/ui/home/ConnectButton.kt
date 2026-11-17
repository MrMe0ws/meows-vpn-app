package app.meows.desktop.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.meows.desktop.ConnState
import app.meows.desktop.ui.components.hand
import app.meows.desktop.ui.Res

/** Кнопка подключения: спящий кот → вращающееся кольцо → лапка в цвете темы внутри светящегося кольца. */
@Composable
fun ConnectButton(state: ConnState, size: Dp, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.outlineVariant
    val plate = MaterialTheme.colorScheme.surfaceContainerHigh
    val inf = rememberInfiniteTransition(label = "btn")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "spin")
    val pulse by inf.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "pulse")
    val glow by inf.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "glow")

    Box(
        Modifier.size(size).clip(CircleShape).clickable(remember { MutableInteractionSource() }, indication = null) { onClick() }.hand(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx(); val inset = stroke / 2 + 2.dp.toPx()
            val arc = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            if (state == ConnState.Connected) drawCircle(Brush.radialGradient(listOf(primary.copy(alpha = glow), Color.Transparent)), radius = this.size.minDimension / 2)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            when (state) {
                ConnState.Connected -> drawArc(primary, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                ConnState.Connecting -> drawArc(Brush.sweepGradient(listOf(Color.Transparent, tertiary, primary)), spin, 270f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                else -> {}
            }
        }
        Box(Modifier.fillMaxSize().padding(size * 0.09f).clip(CircleShape).background(plate), contentAlignment = Alignment.Center) {
            Crossfade(state == ConnState.Idle, label = "cat") { sleeping ->
                if (sleeping) Box(Modifier.fillMaxSize()) {
                    Image(Res.bitmap("sleep_cat.png"), "Кот спит — нажмите, чтобы подключиться", Modifier.fillMaxSize().padding(size * 0.1f))
                    SleepZzz(size, Modifier.align(Alignment.TopEnd).padding(top = size * 0.12f, end = size * 0.16f))
                }
                else Image(
                    Res.bitmap("paw.png"), "Подключение", Modifier.fillMaxSize().padding(size * 0.22f).scale(if (state == ConnState.Connected) pulse else 1f),
                    colorFilter = ColorFilter.tint(primary), alpha = if (state == ConnState.Connected) 1f else 0.45f + glow,
                )
            }
        }
    }
}