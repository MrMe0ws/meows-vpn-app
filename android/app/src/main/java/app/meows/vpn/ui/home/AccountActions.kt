package app.meows.vpn.ui.home

import app.meows.vpn.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.shop.ShopLinks

/**
 * Быстрые действия магазина под подпиской: «Продлить» (когда ≤ 7 дней или закончилась) или «Кабинет», и «Поддержка».
 * Ссылки — из заголовков подписки (см. [ShopLinks]).
 */
@Composable
fun AccountActions(vm: MeowsViewModel, st: XKStore, onSupport: () -> Unit) {
    vm.rev
    val renew = ShopLinks.needsRenewal(st)
    val cabinet = ShopLinks.cabinet(st)
    val hasSupport = ShopLinks.support(st) != null
    if (cabinet == null && !hasSupport) return
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (cabinet != null && renew) {
            Button(onClick = { vm.openLink(cabinet) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Autorenew, null); Spacer(Modifier.width(8.dp)); Text(tr("Продлить"))
            }
        } else if (cabinet != null) {
            FilledTonalButton(onClick = { vm.openLink(cabinet) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.AccountCircle, null); Spacer(Modifier.width(8.dp)); Text(tr("Кабинет"))
            }
        }
        if (hasSupport) FilledTonalButton(onClick = onSupport, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.SupportAgent, null); Spacer(Modifier.width(8.dp)); Text(tr("Поддержка"))
        }
    }
}
