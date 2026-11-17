package app.meows.desktop.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Три «z», по очереди всплывающие над спящим котом (как в Android-версии). [button] — размер кнопки: от него зависят размеры букв и путь. */
@Composable
fun SleepZzz(button: Dp, modifier: Modifier = Modifier) {
    val k = button.value / 200f   // Android-значения подобраны для кнопки 200 dp
    val inf = rememberInfiniteTransition(label = "zzz")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "t")
    val color = MaterialTheme.colorScheme.primary
    Box(modifier.size((48 * k).dp)) {
        listOf(0f to 14, 0.33f to 20, 0.66f to 26).forEach { (phase, sp) ->
            val p = (t + phase) % 1f
            Text(
                "z", color = color, fontSize = (sp * k).sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (p * 22 * k).dp, y = (-p * 30 * k).dp)
                    .alpha(if (p < 0.15f) p / 0.15f else (1f - p) / 0.85f),
            )
        }
    }
}
