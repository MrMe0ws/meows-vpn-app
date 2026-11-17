package app.meows.desktop.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.DropdownMenu
import app.meows.desktop.ui.components.DropdownMenuItem
import androidx.compose.material3.Icon
import app.meows.desktop.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.ui.components.hand
import app.meows.desktop.vpn.VpnMode

/** Выбор режима (TUN / системный прокси) с кнопкой «что это?». Сменить можно и во время подключения — переподключимся. */
@Composable
fun ModePicker(mode: VpnMode, enabled: Boolean, onPick: (VpnMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            Surface(
                onClick = { open = true }, enabled = enabled, shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().hand(enabled),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Режим", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(mode.title, style = MaterialTheme.typography.titleMedium)
                    }
                    Icon(Icons.Rounded.ExpandMore, null)
                }
            }
            DropdownMenu(open, onDismissRequest = { open = false }, modifier = Modifier.width(340.dp)) {
                VpnMode.entries.forEach { m ->
                    DropdownMenuItem(
                        text = { Column { Text(m.title, style = MaterialTheme.typography.titleSmall); Text(m.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                        trailingIcon = { if (m == mode) Icon(Icons.Rounded.Check, "Выбран", tint = MaterialTheme.colorScheme.primary) },
                        onClick = { open = false; onPick(m) },
                    )
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { info = true }) { Icon(Icons.Rounded.Info, "Что такое режимы") }
    }
    if (info) ModeInfoDialog { info = false }
}
