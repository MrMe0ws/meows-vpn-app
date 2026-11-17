package app.meows.vpn.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.R

/** Шестерёнка слева, название по центру, «+» справа (меню способов добавления подписки). */
@Composable
fun HomeTopBar(vm: MeowsViewModel, openSettings: () -> Unit, onScanQr: () -> Unit, onManual: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, "Настройки") }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo_icon), null, Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text("Мяус VPN", style = MaterialTheme.typography.titleLarge)
        }
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
    }
}
