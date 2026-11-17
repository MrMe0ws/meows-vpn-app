package app.meows.desktop.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Нижняя плавающая панель: настройки · добавить подписку · пинг всех · обновить (как в Android-версии). */
@Composable
fun HomeToolbar(
    hasSub: Boolean, pinging: Boolean, refreshing: Boolean,
    onSettings: () -> Unit, onAdd: () -> Unit, onPing: () -> Unit, onRefresh: () -> Unit, modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val colors = IconButtonDefaults.iconButtonColors(contentColor = cs.onPrimaryContainer)
    Surface(modifier.padding(bottom = 16.dp), shape = RoundedCornerShape(28.dp), color = cs.primaryContainer, shadowElevation = 6.dp) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            IconButton(onClick = onSettings, colors = colors) { Icon(Icons.Rounded.Settings, "Настройки") }
            IconButton(onClick = onAdd, colors = colors) { Icon(Icons.Rounded.Add, "Добавить подписку") }
            if (hasSub) {
                IconButton(onClick = onPing, enabled = !pinging, colors = colors) {
                    if (pinging) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = cs.onPrimaryContainer) else Icon(Icons.Rounded.Speed, "Пинг всех серверов")
                }
                IconButton(onClick = onRefresh, enabled = !refreshing, colors = colors) {
                    if (refreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = cs.onPrimaryContainer) else Icon(Icons.Rounded.Refresh, "Обновить подписку")
                }
            }
        }
    }
}