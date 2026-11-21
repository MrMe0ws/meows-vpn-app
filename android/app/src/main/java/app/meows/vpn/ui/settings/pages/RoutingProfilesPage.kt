package app.meows.vpn.ui.settings.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.i18n.tr
import app.meows.vpn.i18n.trf
import app.meows.vpn.routing.CustomProfiles
import app.meows.vpn.routing.RouteProfile
import app.meows.vpn.routing.RoutingProfiles
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold

/** Идентификатор «нового» профиля для редактора: сохранение создаёт запись. */
const val NEW_PROFILE = "new"

/** Идентификатор для редактора в режиме «только просмотр»: показывает правила профиля подписки. */
const val VIEW_PROFILE = "view"

/** Название выбранного профиля — для строки «Профили маршрутизации» на странице маршрутизации. */
fun routeProfileTitle(vm: MeowsViewModel): String {
    vm.rev
    return RoutingProfiles.title(vm.st, RoutingProfiles.selectedId(vm.st), tr("Профиль подписки"), tr("Мяус"))
}

private fun builtinDesc(id: String) = if (id == RoutingProfiles.ROSCOM) tr("РФ напрямую, YouTube и Telegram через VPN, реклама блокируется") else tr("Россия напрямую, остальное через VPN")

/**
 * Профили маршрутизации: «Из подписки», встроенные («Мяус», RoscomVPN) и свои. Выбор переподключает VPN.
 * Встроенные и свои можно править, копировать, отправлять ссылкой и QR; свои — удалять; изменённые встроенные — сбрасывать.
 */
@Composable
fun RoutingProfilesPage(vm: MeowsViewModel, onBack: () -> Unit, openEdit: (String) -> Unit) {
    vm.rev
    val st = vm.st
    val ctx = LocalContext.current
    val selected = RoutingProfiles.selectedId(st)
    val busy = vm.isApplying(XKStore.P_ROUTE_PROFILE)
    val allVpn = vm.flag(XKStore.P_ALL_VPN, false)
    val footer = if (allVpn) tr("Сейчас включён режим «Весь трафик через VPN»: профиль не действует.")
    else tr("Выбранный профиль заменяет правила подписки. «Российские приложения напрямую», блок рекламы и личные исключения продолжают действовать поверх него.")
    val copySuffix = tr("(копия)")
    var deleting by remember { mutableStateOf<RouteProfile?>(null) }
    var qr by remember { mutableStateOf<RouteProfile?>(null) }
    var importing by remember { mutableStateOf(false) }
    val subName = RoutingProfiles.subscriptionName(st) ?: tr("Профиль подписки")

    SettingsPageScaffold(
        tr("Профили"), onBack,
        actions = {
            IconButton(onClick = { importing = true }) { Icon(Icons.Rounded.ContentPaste, tr("Добавить профиль")) }
            IconButton(onClick = { openEdit(NEW_PROFILE) }) { Icon(Icons.Rounded.Add, tr("Новый профиль")) }
        },
    ) {
        SettingsGroup(footer = footer) {
            ProfileRow(
                subName, tr("Правила вашей подписки"), listOf(tr("Подписка") to true, tr("Чтение") to false),
                selected == RoutingProfiles.SUBSCRIPTION, busy, ProfileActions(onCopy = { RoutingProfiles.copyAsCustom(st, RoutingProfiles.subscriptionAsProfile(st), subName, copySuffix); vm.bump() }, onView = { openEdit(VIEW_PROFILE) }),
            ) { vm.setRouteProfile(RoutingProfiles.SUBSCRIPTION) }
            RoutingProfiles.BUILTIN_IDS.forEach { id ->
                val p = RoutingProfiles.builtin(st, id)
                val changed = RoutingProfiles.isOverridden(st, id)
                ProfileRow(
                    p.name, builtinDesc(id), listOf(tr("Встроенный") to true) + (if (changed) listOf(tr("Изменён") to false) else emptyList()),
                    selected == id, busy,
                    ProfileActions(
                        onCopy = { RoutingProfiles.copyAsCustom(st, p, p.name, copySuffix); vm.bump() },
                        onShare = { shareProfile(ctx, p) }, onQr = { qr = p }, onEdit = { openEdit(id) },
                        onReset = if (changed) ({ RoutingProfiles.resetBuiltin(st, id); if (selected == id) vm.applyRouteChange(); vm.bump() }) else null,
                    ),
                ) { vm.setRouteProfile(id) }
            }
            CustomProfiles.list(st).forEach { p ->
                val id = RoutingProfiles.customId(p)
                ProfileRow(
                    p.name.ifBlank { tr("Без названия") }, trf("Правил: %d", p.ruleCount), listOf(tr("Свой") to false),
                    selected == id, busy,
                    ProfileActions(
                        onCopy = { RoutingProfiles.copyAsCustom(st, p, p.name, copySuffix); vm.bump() },
                        onShare = { shareProfile(ctx, p) }, onQr = { qr = p }, onEdit = { openEdit(p.id) }, onDelete = { deleting = p },
                    ),
                ) { vm.setRouteProfile(id) }
            }
        }
    }
    qr?.let { ProfileQrDialog(it) { qr = null } }
    if (importing) ProfileImportDialog(
        onImport = { text ->
            val p = RouteProfile.fromLink(text)?.takeIf { it.ruleCount > 0 || it.name.isNotBlank() }
            if (p == null) tr("Не похоже на профиль маршрутизации")
            else { CustomProfiles.save(st, p.copy(id = "", name = p.name.ifBlank { tr("Импортированный профиль") })); vm.bump(); null }
        },
        onDismiss = { importing = false },
    )
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Удалить профиль?")) },
            text = { Text(trf("«%s» будет удалён. Это нельзя отменить.", p.name.ifBlank { tr("Без названия") })) },
            confirmButton = {
                TextButton(onClick = {
                    val wasSelected = selected == RoutingProfiles.customId(p)
                    CustomProfiles.delete(st, p.id); deleting = null
                    if (wasSelected) vm.setRouteProfile(RoutingProfiles.SUBSCRIPTION) else vm.bump()
                }) { Text(tr("Удалить")) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(tr("Отмена")) } },
        )
    }
}
