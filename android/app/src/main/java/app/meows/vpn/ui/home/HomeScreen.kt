package app.meows.vpn.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.ui.components.CatConnectButton

/**
 * Главная: всё на одном экране — кнопка подключения, подписка и список серверов.
 * Настройки — отдельный экран (шестерёнка), добавление подписки — меню «+».
 */
@Composable
fun HomeScreen(vm: MeowsViewModel, onToggle: () -> Unit, openSettings: () -> Unit, onScanQr: () -> Unit) {
    val servers = vm.servers
    val selected = vm.selected
    var expanded by rememberSaveable { mutableStateOf(true) }
    var manualEntry by remember { mutableStateOf(false) }
    var supportSheet by remember { mutableStateOf(false) }
    var configOf by remember { mutableIntStateOf(-1) }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
    ) {
        item { HomeTopBar() }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CatConnectButton(vm.vpnState, size = 200.dp, onClick = onToggle)
                Spacer(Modifier.height(12.dp))
                StatusText(vm.vpnState)
                Spacer(Modifier.height(16.dp))
            }
        }

        if (!vm.hasSub) {
            item {
                EmptyCard(
                    Modifier.padding(horizontal = 16.dp),
                    onPaste = { vm.addFromClipboard() },
                    onScan = onScanQr,
                    onManual = { manualEntry = true },
                )
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
                    Text(
                        if (expanded) "Скрыть все" else "Показать все",
                        Modifier.clickable { expanded = !expanded }.padding(vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SubscriptionHeader(
                    vm, expanded, onExpand = { expanded = !expanded }, onReplace = { manualEntry = true }, onSupport = { supportSheet = true },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            if (expanded) {
                itemsIndexed(servers) { i, s ->
                    ServerRow(
                        server = s, selected = i == selected, last = i == servers.lastIndex,
                        ping = vm.pingOf(i), pinging = i in vm.pingingOne,
                        onSelect = { vm.select(i) }, onPing = { vm.pingOne(i) }, onConfig = { configOf = i },
                    )
                }
            }
        }
    }
    HomeToolbar(
        vm, hasSub = vm.hasSub, openSettings = openSettings, onScanQr = onScanQr, onManual = { manualEntry = true },
        modifier = Modifier.align(Alignment.BottomCenter),
    )
    }

    if (manualEntry) AddSubscriptionSheet(vm) { manualEntry = false }
    if (supportSheet) SupportSheet(vm) { supportSheet = false }
    if (configOf >= 0) ConfigDialog(vm, configOf) { configOf = -1 }
}
