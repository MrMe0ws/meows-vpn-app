package app.meows.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.meows.desktop.AppModel
import app.meows.desktop.i18n.tr
import app.meows.desktop.i18n.trf
import app.meows.desktop.routing.RouteProfile
import app.meows.desktop.routing.RoutingProfiles
import app.meows.desktop.ui.components.IconButton
import app.meows.desktop.ui.components.hand

private fun builtinDesc(id: String) = if (id == RoutingProfiles.ROSCOM) tr("РФ напрямую, YouTube и Telegram через VPN, реклама блокируется") else tr("Россия напрямую, остальное через VPN")

/** Название выбранного профиля — для строки «Профили маршрутизации» в настройках. */
internal fun routeProfileTitle(app: AppModel): String {
    app.profilesRev
    val st = app.settings
    return RoutingProfiles.title(st, RoutingProfiles.selectedId(st), app.routingHeader, tr("Профиль подписки"))
}

/**
 * Профили маршрутизации: «Из подписки», встроенные («Мяус», RoscomVPN) и свои. Выбор переподключает VPN.
 * Встроенные и свои можно править, копировать, отдавать ссылкой и QR; свои — удалять; изменённые встроенные — сбрасывать.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(app: AppModel, onBack: () -> Unit, openEdit: (String) -> Unit) {
    app.profilesRev
    val st = app.settings
    val selected = RoutingProfiles.selectedId(st)
    val footer = if (app.allVpn) tr("Сейчас включён режим «Весь трафик через VPN»: профиль не действует.")
    else tr("Выбранный профиль заменяет правила подписки. Блок рекламы и «Все российские сайты напрямую» продолжают действовать поверх него.")
    val copySuffix = tr("(копия)")
    val linkCopied = tr("Ссылка профиля скопирована")
    var deleting by remember { mutableStateOf<RouteProfile?>(null) }
    var qr by remember { mutableStateOf<RouteProfile?>(null) }
    var importing by remember { mutableStateOf(false) }
    val subName = RoutingProfiles.subscriptionName(app.routingHeader) ?: tr("Профиль подписки")
    fun copy(p: RouteProfile, name: String) { RoutingProfiles.copyAsCustom(st, p, name, copySuffix); app.profilesChanged() }
    fun link(p: RouteProfile) { copyToClipboard(p.toLink()); app.toast = linkCopied }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(tr("Профили")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Назад")) } },
                actions = {
                    IconButton(onClick = { importing = true }) { Icon(Icons.Rounded.ContentPaste, tr("Добавить профиль")) }
                    IconButton(onClick = { openEdit(NEW_PROFILE) }) { Icon(Icons.Rounded.Add, tr("Новый профиль")) }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            Group(tr("Профили маршрутизации"), footer) {
                ProfileRow(
                    subName, tr("Правила вашей подписки"), listOf(tr("Подписка") to true, tr("Чтение") to false), selected == RoutingProfiles.SUBSCRIPTION,
                    ProfileActions(onCopy = { copy(RoutingProfiles.subscriptionAsProfile(st, app.routingHeader, app.sub?.body, app.selected), subName) }, onView = { openEdit(VIEW_PROFILE) }),
                ) { app.chooseRouteProfile(RoutingProfiles.SUBSCRIPTION) }
                RoutingProfiles.BUILTIN_IDS.forEach { id ->
                    val p = RoutingProfiles.builtin(st, id)
                    val changed = RoutingProfiles.isOverridden(st, id)
                    ProfileRow(
                        p.name, builtinDesc(id), listOf(tr("Встроенный") to true) + (if (changed) listOf(tr("Изменён") to false) else emptyList()), selected == id,
                        ProfileActions(
                            onCopy = { copy(p, p.name) }, onLink = { link(p) }, onQr = { qr = p }, onEdit = { openEdit(id) },
                            onReset = if (changed) ({ RoutingProfiles.resetBuiltin(st, id); app.profilesChanged(reconnectIfSelected = id) }) else null,
                        ),
                    ) { app.chooseRouteProfile(id) }
                }
                RoutingProfiles.customs(st).forEach { p ->
                    val id = RoutingProfiles.customId(p)
                    ProfileRow(
                        p.name.ifBlank { tr("Без названия") }, trf("Правил: %d", p.ruleCount), listOf(tr("Свой") to false), selected == id,
                        ProfileActions(onCopy = { copy(p, p.name) }, onLink = { link(p) }, onQr = { qr = p }, onEdit = { openEdit(p.id) }, onDelete = { deleting = p }),
                    ) { app.chooseRouteProfile(id) }
                }
            }
        }
    }
    qr?.let { ProfileQrDialog(it) { qr = null } }
    if (importing) ProfileImportDialog(
        onImport = { text ->
            val p = RouteProfile.fromLink(text)?.takeIf { it.ruleCount > 0 || it.name.isNotBlank() }
            if (p == null) tr("Не похоже на профиль маршрутизации")
            else { RoutingProfiles.saveCustom(st, p.copy(id = "", name = p.name.ifBlank { tr("Импортированный профиль") })); app.profilesChanged(); null }
        },
        onDismiss = { importing = false },
    )
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Удалить профиль?")) },
            text = { Text(trf("«%s» будет удалён. Это нельзя отменить.", p.name.ifBlank { tr("Без названия") })) },
            confirmButton = {
                TextButton(modifier = Modifier.hand(), onClick = {
                    val wasSelected = selected == RoutingProfiles.customId(p)
                    RoutingProfiles.deleteCustom(st, p.id); deleting = null
                    if (wasSelected) app.chooseRouteProfile(RoutingProfiles.SUBSCRIPTION) else app.profilesChanged()
                }) { Text(tr("Удалить")) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }, modifier = Modifier.hand()) { Text(tr("Отмена")) } },
        )
    }
}
