package app.meows.vpn.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.vpn.XKVpnService
import app.meows.vpn.ui.components.formatTimer
import kotlinx.coroutines.delay

/** «Отключено / Подключаемся… / Подключено» + таймер сессии. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusText(state: Int) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state) {
        while (state == XKVpnService.CONNECTED) { now = System.currentTimeMillis(); delay(1000) }
    }
    AnimatedContent(state, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status") { s ->
        Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (s) {
                XKVpnService.CONNECTED -> {
                    Text("Подключено", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        formatTimer((now - XKVpnService.connectedAt).coerceAtLeast(0)),
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                XKVpnService.CONNECTING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    LoadingIndicator(Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Подключаемся…", style = MaterialTheme.typography.headlineMedium)
                }
                else -> Text("Отключено", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
