package app.meows.vpn.ui.settings

import app.meows.vpn.ui.settings.pages.LanguagePage
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import app.meows.vpn.ui.settings.pages.RoutingPage
import app.meows.vpn.ui.settings.pages.StatsPage
import app.meows.vpn.ui.settings.pages.SubscriptionPage
import app.meows.vpn.ui.settings.pages.UpdatesPage
import app.meows.vpn.ui.settings.pages.UrlSchemesPage

/** Страницы настроек. [parent] — куда ведёт «назад». Новая страница = новый элемент + файл в pages/. */
enum class SettingsPage(val parent: SettingsPage?, val depth: Int) {
    Home(null, 0),
    Appearance(Home, 1), Language(Home, 1), Routing(Home, 1), Subscription(Home, 1), Connection(Home, 1), Ping(Home, 1), Updates(Home, 1), About(Home, 1), Stats(Home, 1), UrlSchemes(About, 2),
    Bypass(Routing, 2), Apps(Routing, 2), AutoSwitch(Connection, 2), MobileBypass(Connection, 2), AppIcon(Appearance, 2),
}

/** Роутер настроек: главная со свёрнутыми разделами → страница раздела. */
@Composable
fun SettingsScreen(vm: MeowsViewModel, onBack: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Home) }
    BackHandler(enabled = page != SettingsPage.Home) { page = page.parent ?: SettingsPage.Home }
    val back = { page = page.parent ?: SettingsPage.Home }

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
        when (p) {
            SettingsPage.Home -> SettingsHome(vm, onBack = onBack, open = { page = it })
            SettingsPage.Appearance -> AppearancePage(vm, back, openAppIcon = { page = SettingsPage.AppIcon })
            SettingsPage.AppIcon -> AppIconPage(back)
            SettingsPage.Language -> LanguagePage(vm, back)
            SettingsPage.Routing -> RoutingPage(vm, back, openBypass = { page = SettingsPage.Bypass }, openApps = { page = SettingsPage.Apps })
            SettingsPage.Subscription -> SubscriptionPage(vm, back)
            SettingsPage.Connection -> ConnectionPage(vm, back, openAutoSwitch = { page = SettingsPage.AutoSwitch }, openMobileBypass = { page = SettingsPage.MobileBypass })
            SettingsPage.AutoSwitch -> AutoSwitchPage(vm, back)
            SettingsPage.MobileBypass -> MobileBypassPage(vm, back)
            SettingsPage.UrlSchemes -> UrlSchemesPage(vm, back)
            SettingsPage.Ping -> PingPage(vm, back)
            SettingsPage.Updates -> UpdatesPage(vm, back)
            SettingsPage.About -> AboutPage(vm, back, openUrlSchemes = { page = SettingsPage.UrlSchemes })
            SettingsPage.Stats -> StatsPage(vm, back)
            SettingsPage.Bypass -> BypassPage(vm, back)
            SettingsPage.Apps -> AppsPage(vm, back)
        }
    }
}
