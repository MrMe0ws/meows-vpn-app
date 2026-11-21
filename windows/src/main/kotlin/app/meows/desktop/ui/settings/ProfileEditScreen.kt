package app.meows.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel
import app.meows.desktop.i18n.tr
import app.meows.desktop.routing.RouteProfile
import app.meows.desktop.routing.RoutingProfiles
import app.meows.desktop.ui.components.IconButton
import app.meows.desktop.ui.components.Switch
import app.meows.desktop.ui.components.hand

/** Идентификатор «нового» профиля для редактора: сохранение создаёт запись. */
internal const val NEW_PROFILE = "new"

/** Идентификатор для редактора в режиме «только просмотр»: показывает правила профиля подписки. */
internal const val VIEW_PROFILE = "view"

private fun String.lines2list() = lines().map { it.trim() }.filter { it.isNotEmpty() }

/** «block-proxy-direct» → «Блок → Прокси → Напрямую». */
private fun orderTitle(order: String) = order.split("-").joinToString(" → ") {
    when (it) { "block" -> tr("Блок"); "proxy" -> tr("Прокси"); else -> tr("Напрямую") }
}

/** Редактор профиля маршрутизации. [id] — id своего или встроенного профиля либо [NEW_PROFILE]. Правки сохраняются кнопкой «Сохранить». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(app: AppModel, id: String, onBack: () -> Unit) {
    val st = app.settings
    val viewOnly = id == VIEW_PROFILE   // профиль подписки: только просмотр
    val builtin = id in RoutingProfiles.BUILTIN_IDS
    val base = remember(id) { if (id == NEW_PROFILE) null else if (viewOnly) RoutingProfiles.subscriptionAsProfile(st, app.routingHeader, app.sub?.body, app.selected) else if (builtin) RoutingProfiles.builtin(st, id) else RoutingProfiles.custom(st, id) } ?: RouteProfile(name = tr("Новый профиль"))
    var name by remember { mutableStateOf(base.name) }
    var global by remember { mutableStateOf(base.globalProxy) }
    var directSites by remember { mutableStateOf(base.directSites.joinToString("\n")) }; var directIp by remember { mutableStateOf(base.directIp.joinToString("\n")) }
    var proxySites by remember { mutableStateOf(base.proxySites.joinToString("\n")) }; var proxyIp by remember { mutableStateOf(base.proxyIp.joinToString("\n")) }
    var blockSites by remember { mutableStateOf(base.blockSites.joinToString("\n")) }; var blockIp by remember { mutableStateOf(base.blockIp.joinToString("\n")) }
    var order by remember { mutableStateOf(base.routeOrder) }; var strategy by remember { mutableStateOf(base.domainStrategy) }
    var remoteType by remember { mutableStateOf(base.remoteType) }; var remoteDomain by remember { mutableStateOf(base.remoteDomain) }; var remoteIp by remember { mutableStateOf(base.remoteIp) }
    var geoip by remember { mutableStateOf(base.geoipUrl) }; var geosite by remember { mutableStateOf(base.geositeUrl) }
    val isSelected = id != NEW_PROFILE && RoutingProfiles.selectedId(st) == (if (builtin) id else RoutingProfiles.customId(base))
    var localType by remember { mutableStateOf(base.localType) }; var localDomain by remember { mutableStateOf(base.localDomain) }; var localIp by remember { mutableStateOf(base.localIp) }

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
        val savedId = if (builtin) { RoutingProfiles.saveBuiltin(st, edited); id } else RoutingProfiles.customId(RoutingProfiles.saveCustom(st, edited))
        app.profilesChanged(reconnectIfSelected = savedId)   // правка выбранного профиля действует после переподключения
        onBack()
    }

    CompositionLocalProvider(LocalEditReadOnly provides viewOnly) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(if (id == NEW_PROFILE) tr("Новый профиль") else if (viewOnly) tr("Профиль подписки") else tr("Изменить профиль")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Назад")) } },
                actions = { if (!viewOnly) TextButton(onClick = ::save, modifier = Modifier.hand()) { Text(tr("Сохранить")) } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            if (viewOnly) { EditTitle(tr("Только просмотр")); EditCard { Text(tr("Это правила шаблона вашей подписки для выбранного сервера (у других серверов они могут отличаться). Править их нельзя, но можно скопировать в свой профиль."), style = MaterialTheme.typography.bodyMedium) } }
            EditTitle(tr("Основные"))
            EditCard {
                if (builtin || viewOnly) Text(name.ifBlank { tr("Профиль подписки") }, style = MaterialTheme.typography.titleMedium) else TextRow(tr("Название профиля"), name, { name = it })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Глобальный прокси"))
                        Text(tr("Всё, чего нет в списках, идёт через VPN. Выключено — напрямую."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(global, { global = it }, enabled = !viewOnly)
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
                GeoSection(app, isSelected, geoip, { geoip = it }, geosite, { geosite = it })
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
    }
}
