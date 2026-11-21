package app.meows.vpn.ui.settings

import app.meows.vpn.ui.settings.pages.LanguagePage
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.ui.settings.pages.AboutPage
import app.meows.vpn.ui.settings.pages.AppIconPage
import app.meows.vpn.ui.settings.pages.AppearancePage
import app.meows.vpn.ui.settings.pages.AppsPage
import app.meows.vpn.ui.settings.pages.AutoSwitchPage
import app.meows.vpn.ui.settings.pages.BypassPage
import app.meows.vpn.ui.settings.pages.ConnectionPage
import app.meows.vpn.ui.settings.pages.MobileBypassPage
import app.meows.vpn.ui.settings.pages.PingPage
import app.meows.vpn.ui.settings.pages.ProfileEditPage
import app.meows.vpn.ui.settings.pages.RoutingPage
import app.meows.vpn.ui.settings.pages.RoutingProfilesPage
import app.meows.vpn.ui.settings.pages.StatsPage
import app.meows.vpn.ui.settings.pages.SubscriptionPage
import app.meows.vpn.ui.settings.pages.UpdatesPage
import app.meows.vpn.ui.settings.pages.UrlSchemesPage

/** Страницы настроек. [parent] — куда ведёт «назад». Новая страница = новый элемент + файл в pages/. */
enum class SettingsPage(val parent: SettingsPage?, val depth: Int) {
    Home(null, 0),
    Appearance(Home, 1), Language(Home, 1), Routing(Home, 1), Subscription(Home, 1), Connection(Home, 1), Ping(Home, 1), Updates(Home, 1), About(Home, 1), Stats(Home, 1), UrlSchemes(About, 2),
    Bypass(Routing, 2), Apps(Routing, 2), Profiles(Routing, 2), ProfileEdit(Profiles, 3), AutoSwitch(Connection, 2), MobileBypass(Connection, 2), AppIcon(Appearance, 2),
}

/**
 * Роутер настроек: главная со свёрнутыми разделами → страница раздела.
 * Прокрутка каждой страницы хранится здесь: при возврате с подстраницы список остаётся на том же месте,
 * при новом заходе вперёд — начинается сверху.
 */
@Composable
fun SettingsScreen(vm: MeowsViewModel, onBack: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Home) }
    val scrolls = SettingsPage.entries.associateWith { rememberSaveable(it.name, saver = ScrollState.Saver) { ScrollState(0) } }
    var editId by rememberSaveable { mutableStateOf("") }   // какой профиль маршрутизации открыт в редакторе
    val scope = rememberCoroutineScope()
    val back = { page = page.parent ?: SettingsPage.Home }
    val go = { target: SettingsPage ->
        scope.launch { scrolls.getValue(target).scrollTo(0) }
        page = target
    }
    BackHandler(enabled = page != SettingsPage.Home) { back() }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState.depth >= initialState.depth)
                (slideInHorizontally { it / 5 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 8 } + fadeOut())
            else
                (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith (slideOutHorizontally { it / 8 } + fadeOut())
        },
        label = "settings",
    ) { p ->
        CompositionLocalProvider(LocalPageScroll provides scrolls.getValue(p)) {
            when (p) {
                SettingsPage.Home -> SettingsHome(vm, onBack = onBack, open = go)
                SettingsPage.Appearance -> AppearancePage(vm, back, openAppIcon = { go(SettingsPage.AppIcon) })
                SettingsPage.AppIcon -> AppIconPage(back)
                SettingsPage.Language -> LanguagePage(vm, back)
                SettingsPage.Routing -> RoutingPage(vm, back, openBypass = { go(SettingsPage.Bypass) }, openApps = { go(SettingsPage.Apps) }, openProfiles = { go(SettingsPage.Profiles) })
                SettingsPage.Subscription -> SubscriptionPage(vm, back)
                SettingsPage.Connection -> ConnectionPage(vm, back, openAutoSwitch = { go(SettingsPage.AutoSwitch) }, openMobileBypass = { go(SettingsPage.MobileBypass) })
                SettingsPage.AutoSwitch -> AutoSwitchPage(vm, back)
                SettingsPage.MobileBypass -> MobileBypassPage(vm, back)
                SettingsPage.UrlSchemes -> UrlSchemesPage(vm, back)
                SettingsPage.Ping -> PingPage(vm, back)
                SettingsPage.Updates -> UpdatesPage(vm, back)
                SettingsPage.About -> AboutPage(vm, back, openUrlSchemes = { go(SettingsPage.UrlSchemes) })
                SettingsPage.Stats -> StatsPage(vm, back)
                SettingsPage.Bypass -> BypassPage(vm, back)
                SettingsPage.Apps -> AppsPage(vm, back)
                SettingsPage.Profiles -> RoutingProfilesPage(vm, back, openEdit = { editId = it; go(SettingsPage.ProfileEdit) })
                SettingsPage.ProfileEdit -> ProfileEditPage(vm, editId, back)
            }
        }
    }
}
