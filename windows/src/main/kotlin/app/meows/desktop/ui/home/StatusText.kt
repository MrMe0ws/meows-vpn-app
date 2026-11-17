package app.meows.desktop.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import app.meows.desktop.ConnState
import app.meows.desktop.ui.formatTimer
import kotlinx.coroutines.delay

/** «Отключено / Подключаемся… / Подключено» + таймер сессии. */
@Composable
fun StatusText(state: ConnState, connectedAt: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state) { while (state == ConnState.Connected) { now = System.currentTimeMillis(); delay(1000) } }
    Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when (state) {
            ConnState.Connected -> {
                Text("Подключено", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(formatTimer((now - connectedAt).coerceAtLeast(0)), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ConnState.Connecting -> Text("Подключаемся…", style = MaterialTheme.typography.headlineMedium)
            ConnState.Idle -> Text("Отключено", style = MaterialTheme.typography.headlineMedium)
        }
    }
}