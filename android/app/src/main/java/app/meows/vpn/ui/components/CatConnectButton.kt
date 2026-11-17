package app.meows.vpn.ui.components

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meows.vpn.R
import app.meows.vpn.XKVpnService

/**
 * Кот-кнопка подключения. Отключено — кот спит в лежанке и сопит «Zzz»;
 * подключаемся — кольцо крутится; подключено — кот проснулся, кольцо светится.
 */
@Composable
fun CatConnectButton(state: Int, size: Dp, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.outlineVariant
    val plate = MaterialTheme.colorScheme.surfaceContainerHigh

    val inf = rememberInfiniteTransition(label = "cat")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "spin")
    val pulse by inf.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "pulse")
    val glow by inf.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "glow")
    val press = remember { MutableInteractionSource() }

    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = press, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            if (state == XKVpnService.CONNECTED) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(primary.copy(alpha = glow), Color.Transparent)),
                    radius = this.size.minDimension / 2,
                )
            }
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            when (state) {
                XKVpnService.CONNECTED ->
                    drawArc(primary, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                XKVpnService.CONNECTING ->
                    drawArc(
                        Brush.sweepGradient(listOf(Color.Transparent, tertiary, primary)),
                        spin, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                    )
            }
        }

        Crossfade(targetState = state == XKVpnService.IDLE, label = "catstate", modifier = Modifier.fillMaxSize()) { sleeping ->
            if (sleeping) {
                Box(Modifier.fillMaxSize().padding(size * 0.09f).clip(CircleShape).background(plate), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.sleep_cat),
                        contentDescription = "Кот спит — нажмите, чтобы подключиться",
                        modifier = Modifier.fillMaxSize().padding(size * 0.1f),
                    )
                    Zzz(Modifier.align(Alignment.TopEnd).padding(top = size * 0.12f, end = size * 0.16f))
                }
            } else {
                Box(Modifier.fillMaxSize().padding(size * 0.09f).clip(CircleShape).background(plate), contentAlignment = Alignment.Center) {
                    // лапка красится в цвет выбранной темы (primary)
                    Image(
                        painter = painterResource(R.drawable.paw),
                        contentDescription = "Подключение Meows",
                        colorFilter = ColorFilter.tint(primary),
                        alpha = if (state == XKVpnService.CONNECTED) 1f else 0.45f + glow,
                        modifier = Modifier.fillMaxSize().padding(size * 0.22f).scale(if (state == XKVpnService.CONNECTED) pulse else 1f),
                    )
                }
            }
        }
    }
}

/** Три «z», по очереди всплывающие над спящим котом. */
@Composable
private fun Zzz(modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "zzz")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "t")
    val color = MaterialTheme.colorScheme.primary
    Box(modifier.size(48.dp)) {
        listOf(0f to 14, 0.33f to 20, 0.66f to 26).forEach { (phase, sp) ->
            val p = (t + phase) % 1f
            Text(
                "z", color = color, fontSize = sp.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (p * 22).dp, y = (-p * 30).dp)
                    .alpha(if (p < 0.15f) p / 0.15f else (1f - p) / 0.85f),
            )
        }
    }
}
