package app.meows.vpn.ui.settings.pages

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.i18n.Lang
import app.meows.vpn.i18n.tr
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold

/** Название выбранного языка для строки в настройках. */
fun languageName(): String = when (Lang.choice) {
    Lang.RU -> "Русский"
    Lang.EN -> "English"
    else -> tr("Авто")
}

/** Язык интерфейса: авто (по языку системы), русский, английский. */
@Composable
fun LanguagePage(vm: MeowsViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    SettingsPageScaffold(tr("Язык"), onBack) {
        SettingsGroup {
            listOf(Lang.AUTO to tr("Автоматически (язык системы)"), Lang.RU to "Русский", Lang.EN to "English").forEach { (value, name) ->
                ListItem(
                    modifier = Modifier.clickable { Lang.set(ctx, value) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { RadioButton(Lang.choice == value, onClick = null) },
                    headlineContent = { Text(name) },
                )
            }
        }
    }
}
