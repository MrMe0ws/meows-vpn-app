package app.meows.vpn.ui.settings.pages

import androidx.compose.runtime.Composable
import app.meows.vpn.i18n.tr
import app.meows.vpn.ui.settings.SettingsPageScaffold

/** «Оформление» → «Иконка приложения»: выбор иконки ярлыка (применяется, когда приложение свёрнуто). */
@Composable
fun AppIconPage(onBack: () -> Unit) {
    SettingsPageScaffold(tr("Иконка приложения"), onBack) { AppIconPicker() }
}
