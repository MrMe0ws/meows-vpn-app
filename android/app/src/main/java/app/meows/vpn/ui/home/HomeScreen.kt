package app.meows.vpn.ui.home

import app.meows.vpn.i18n.tr
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.components.CatConnectButton

/**
 * Главная: всё на одном экране — кнопка подключения, подписка и список серверов.
 * Настройки — отдельный экран (шестерёнка), добавление подписки — меню «+».
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(vm: MeowsViewModel, onToggle: () -> Unit, openSettings: () -> Unit, onScanQr: () -> Unit) {
    val subs = vm.subs
    var manualEntry by remember { mutableStateOf(false) }
    var replaceOf by remember { mutableStateOf<XKStore?>(null) }
    var supportOf by remember { mutableStateOf<XKStore?>(null) }
    var configOf by remember { mutableStateOf<Pair<XKStore, Int>?>(null) }
    val collapsed = remember { mutableStateListOf<Int>() }   // карточки, свёрнутые пользователем (по номеру слота)

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
    // шапка с кнопкой подключения закреплена и не меняется при прокрутке: скроллится только список подписок и серверов
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CatConnectButton(vm.vpnState, size = 200.dp, onClick = onToggle)
        Spacer(Modifier.height(12.dp))
        StatusText(vm.vpnState)
        Spacer(Modifier.height(16.dp))
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(), state = listState,
        contentPadding = PaddingValues(bottom = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
    ) {

        if (subs.isEmpty()) {
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
                val allOpen = collapsed.isEmpty()
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
                    Text(
                        if (allOpen) tr("Скрыть все") else tr("Показать все"),
                        Modifier.clickable { if (allOpen) { collapsed.clear(); collapsed.addAll(subs.map { it.id() }) } else collapsed.clear() }.padding(vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            subs.forEach { store ->
                val id = store.id()
                val expanded = id !in collapsed
                val list = store.servers()
                item(key = "head$id") {
                    SubscriptionHeader(
                        vm, store, expanded, onExpand = { if (expanded) collapsed.add(id) else collapsed.remove(id) },
                        onReplace = { replaceOf = store }, onSupport = { supportOf = store },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                if (expanded) {
                    itemsIndexed(list, key = { i, _ -> "srv${id}_$i" }) { i, s ->
                        ServerRow(
                            server = s, selected = vm.isActive(store) && i == store.index(), last = i == list.lastIndex,
                            ping = vm.pingOf(i, store), pinging = vm.isPingingOne(store, i),
                            onSelect = { vm.select(store, i) }, onPing = { vm.pingOne(i, store) }, onConfig = { configOf = store to i },
                        )
                    }
                }
                item(key = "gap$id") { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
    }
    HomeToolbar(
        vm, openSettings = openSettings, onScanQr = onScanQr, onManual = { manualEntry = true },
        modifier = Modifier.align(Alignment.BottomCenter),
    )
    }

    if (manualEntry) AddSubscriptionSheet(vm) { manualEntry = false }
    replaceOf?.let { s -> AddSubscriptionSheet(vm, replace = s) { replaceOf = null } }
    supportOf?.let { s -> SupportSheet(vm, s) { supportOf = null } }
    configOf?.let { (s, i) -> ConfigDialog(vm, s, i) { configOf = null } }
}
