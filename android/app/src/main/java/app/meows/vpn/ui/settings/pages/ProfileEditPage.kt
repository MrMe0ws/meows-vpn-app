package app.meows.vpn.ui.settings.pages

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.i18n.tr
import app.meows.vpn.routing.CustomProfiles
import app.meows.vpn.routing.RouteProfile
import app.meows.vpn.routing.RoutingProfiles
import app.meows.vpn.ui.settings.SettingsPageScaffold

@Composable
private fun field(init: String): MutableState<String> = rememberSaveable { mutableStateOf(init) }

private fun String.lines2list() = lines().map { it.trim() }.filter { it.isNotEmpty() }

/** Редактор профиля маршрутизации. [id] — id своего или встроенного профиля либо [NEW_PROFILE]. Правки сохраняются кнопкой «Сохранить». */
@Composable
fun ProfileEditPage(vm: MeowsViewModel, id: String, onBack: () -> Unit) {
    val st = vm.st
    val viewOnly = id == VIEW_PROFILE   // профиль подписки: только просмотр
    val builtin = id in RoutingProfiles.BUILTIN_IDS
    val base = remember(id) { if (id == NEW_PROFILE) null else if (viewOnly) RoutingProfiles.subscriptionAsProfile(st) else if (builtin) RoutingProfiles.builtin(st, id) else CustomProfiles.get(st, id) } ?: RouteProfile(name = tr("Новый профиль"))
    var name by field(base.name)
    var global by rememberSaveable { mutableStateOf(base.globalProxy) }
    var directSites by field(base.directSites.joinToString("\n")); var directIp by field(base.directIp.joinToString("\n"))
    var proxySites by field(base.proxySites.joinToString("\n")); var proxyIp by field(base.proxyIp.joinToString("\n"))
    var blockSites by field(base.blockSites.joinToString("\n")); var blockIp by field(base.blockIp.joinToString("\n"))
    var order by field(base.routeOrder); var strategy by field(base.domainStrategy)
    var remoteType by field(base.remoteType); var remoteDomain by field(base.remoteDomain); var remoteIp by field(base.remoteIp)
    var geoip by field(base.geoipUrl); var geosite by field(base.geositeUrl)
    val isSelected = id != NEW_PROFILE && RoutingProfiles.selectedId(st) == (if (builtin) id else RoutingProfiles.customId(base))
    var localType by field(base.localType); var localDomain by field(base.localDomain); var localIp by field(base.localIp)

    fun save() {
        val edited = RouteProfile(
            id = base.id, name = name.trim().ifEmpty { tr("Новый профиль") }, globalProxy = global,
            directSites = directSites.lines2list(), directIp = directIp.lines2list(),
            proxySites = proxySites.lines2list(), proxyIp = proxyIp.lines2list(),
            blockSites = blockSites.lines2list(), blockIp = blockIp.lines2list(),
            routeOrder = order, domainStrategy = strategy,
            remoteType = remoteType, remoteDomain = remoteDomain.trim(), remoteIp = remoteIp.trim(),
            localType = localType, localDomain = localDomain.trim(), localIp = localIp.trim(),
            dnsHosts = base.dnsHosts, geoipUrl = geoip.trim(), geositeUrl = geosite.trim(),
        )
        val selectedNow = if (builtin) { RoutingProfiles.saveBuiltin(st, edited); id } else RoutingProfiles.customId(CustomProfiles.save(st, edited))
        // правка выбранного профиля действует после переподключения
        if (RoutingProfiles.selectedId(st) == selectedNow) vm.applyRouteChange()
        vm.bump(); onBack()
    }

    CompositionLocalProvider(LocalEditReadOnly provides viewOnly) {
    SettingsPageScaffold(
        if (id == NEW_PROFILE) tr("Новый профиль") else if (viewOnly) tr("Профиль подписки") else tr("Изменить профиль"), onBack,
        actions = { if (!viewOnly) TextButton(onClick = ::save) { Text(tr("Сохранить")) } },
    ) {
        if (viewOnly) { EditTitle(tr("Только просмотр")); EditCard { Text(tr("Это правила шаблона вашей подписки для выбранного сервера (у других серверов они могут отличаться). Править их нельзя, но можно скопировать в свой профиль."), style = MaterialTheme.typography.bodyMedium) } }
        EditTitle(tr("Основные"))
        EditCard {
            if (builtin || viewOnly) Text(name.ifBlank { tr("Профиль подписки") }, style = MaterialTheme.typography.titleMedium) else TextRow(tr("Название профиля"), name, { name = it })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text(tr("Глобальный прокси"))
                    Text(tr("Всё, чего нет в списках, идёт через VPN. Выключено — напрямую."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(global, { global = it }, Modifier.padding(start = 12.dp), enabled = !viewOnly)
            }
        }
        EditTitle(tr("DNS"))
        EditCard {
            DnsPicker(tr("Удалённый DNS (для трафика через VPN)"), MaterialTheme.colorScheme.primary, remoteType, { remoteType = it }, remoteDomain, { remoteDomain = it }, remoteIp, { remoteIp = it })
            HorizontalDivider()
            DnsPicker(tr("Локальный DNS (для прямых доменов)"), MaterialTheme.colorScheme.tertiary, localType, { localType = it }, localDomain, { localDomain = it }, localIp, { localIp = it })
            Text(tr("Если адрес не указан, DNS берётся из подписки."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        EditTitle(tr("Правила"))
        FoldCard(tr("Напрямую (без туннеля)"), MaterialTheme.colorScheme.tertiary) {
            ListField(tr("Домены"), directSites, "geosite:category-ru\ndomain:example.com") { directSites = it }
            ListField(tr("IP-адреса"), directIp, "geoip:direct\n10.0.0.0/8") { directIp = it }
        }
        FoldCard(tr("Через VPN"), MaterialTheme.colorScheme.primary) {
            ListField(tr("Домены"), proxySites, "geosite:youtube\ndomain:telegram.org") { proxySites = it }
            ListField(tr("IP-адреса"), proxyIp, "8.8.8.8\n1.1.1.0/24") { proxyIp = it }
        }
        FoldCard(tr("Блокировать"), MaterialTheme.colorScheme.error) {
            ListField(tr("Домены"), blockSites, "geosite:category-ads\nad.example.com") { blockSites = it }
            ListField(tr("IP-адреса"), blockIp, "1.2.3.4\n5.6.7.0/24") { blockIp = it }
        }
        EditTitle(tr("Расширенные"))
        EditCard {
            RadioChoice(tr("Порядок правил"), RouteProfile.ORDERS.map { Triple(it, orderTitle(it), null) }, order) { order = it }
            HorizontalDivider()
            RadioChoice(
                tr("Стратегия доменов"),
                listOf(
                    Triple("AsIs", tr("Как есть"), tr("Не разрешать домены в IP")),
                    Triple("IPIfNonMatch", tr("IP если не совпадает"), tr("Разрешать, если правила не совпадают")),
                    Triple("IPOnDemand", tr("Всегда IP"), tr("Всегда разрешать домены в IP")),
                ), strategy,
            ) { strategy = it }
        }
        EditTitle(tr("Гео-данные"))
        FoldCard(tr("Файлы geoip и geosite"), MaterialTheme.colorScheme.onSurface) {
            GeoSection(vm, isSelected, geoip, { geoip = it }, geosite, { geosite = it })
        }
    }
    }
}

/** «block-proxy-direct» → «Блок → Прокси → Напрямую». */
private fun orderTitle(order: String) = order.split("-").joinToString(" → ") {
    when (it) { "block" -> tr("Блок"); "proxy" -> tr("Прокси"); else -> tr("Напрямую") }
}
