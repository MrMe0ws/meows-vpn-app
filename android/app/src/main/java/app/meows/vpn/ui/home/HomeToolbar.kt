package app.meows.vpn.ui.home

import app.meows.vpn.i18n.tr
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel

/**
 * Нижняя плавающая панель (Material 3 Expressive): все действия в зоне большого пальца.
 * Только Настройки и Добавить подписку (меню: буфер / QR / вручную). Обновление и пинг живут в шапке
 * подписки ([SubscriptionHeader]) — они относятся к конкретной подписке.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeToolbar(
    vm: MeowsViewModel, openSettings: () -> Unit, onScanQr: () -> Unit, onManual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    // как в Google Фото: невысокая (48dp) панель, всегда на месте; цвет темы (primaryContainer), чтобы не сливалась с фоном
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.primaryContainer,
            toolbarContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier.navigationBarsPadding().padding(bottom = 12.dp),
    ) {
        IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, tr("Настройки")) }
        Box {
            // пока открыто меню — «+» подсвечен (выбран); в покое обе кнопки равнозначны
            val bg by animateColorAsState(if (menu) MaterialTheme.colorScheme.primary else Color.Transparent, label = "addBg")
            val fg by animateColorAsState(
                if (menu) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer, label = "addFg",
            )
            IconButton(onClick = { menu = true }, colors = IconButtonDefaults.iconButtonColors(containerColor = bg, contentColor = fg)) {
                Icon(Icons.Rounded.Add, tr("Добавить подписку"))
            }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(tr("Вставить из буфера обмена")) }, leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) },
                    onClick = { menu = false; vm.addFromClipboard() },
                )
                DropdownMenuItem(
                    text = { Text(tr("QR-код")) }, leadingIcon = { Icon(Icons.Rounded.QrCodeScanner, null) },
                    onClick = { menu = false; onScanQr() },
                )
                DropdownMenuItem(
                    text = { Text(tr("Ввести вручную")) }, leadingIcon = { Icon(Icons.Rounded.Keyboard, null) },
                    onClick = { menu = false; onManual() },
                )
            }
        }
    }
}