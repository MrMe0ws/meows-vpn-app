package app.meows.vpn.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel

/**
 * Нижняя плавающая панель (Material 3 Expressive): все действия в зоне большого пальца.
 * Настройки · Добавить подписку (меню: буфер / QR / вручную) · Пинг всех · Обновить подписку.
 * Пинг и обновление показываются, только когда подписка есть.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeToolbar(vm: MeowsViewModel, hasSub: Boolean, openSettings: () -> Unit, onScanQr: () -> Unit, onManual: () -> Unit, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    HorizontalFloatingToolbar(expanded = true, modifier = modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
        IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, "Настройки") }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.Add, "Добавить подписку") }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Вставить из буфера обмена") }, leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) },
                    onClick = { menu = false; vm.addFromClipboard() },
                )
                DropdownMenuItem(
                    text = { Text("QR-код") }, leadingIcon = { Icon(Icons.Rounded.QrCodeScanner, null) },
                    onClick = { menu = false; onScanQr() },
                )
                DropdownMenuItem(
                    text = { Text("Ввести вручную") }, leadingIcon = { Icon(Icons.Rounded.Keyboard, null) },
                    onClick = { menu = false; onManual() },
                )
            }
        }
        if (hasSub) {
            IconButton(onClick = vm::ping, enabled = !vm.pinging) {
                if (vm.pinging) LoadingIndicator(Modifier.size(24.dp)) else Icon(Icons.Rounded.Speed, "Пинг всех серверов")
            }
            val inf = rememberInfiniteTransition(label = "refresh")
            val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "rot")
            IconButton(onClick = { vm.refresh(false) }, enabled = !vm.refreshing) {
                Icon(Icons.Rounded.Refresh, "Обновить подписку", Modifier.rotate(if (vm.refreshing) rot else 0f))
            }
        }
    }
}