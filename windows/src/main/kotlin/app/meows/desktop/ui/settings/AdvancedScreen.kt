package app.meows.desktop.ui.settings

import app.meows.desktop.i18n.tr
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.meows.desktop.AppModel
import app.meows.desktop.ui.components.IconButton

/** «Для опытных»: настройки, меняющие работу сети. По умолчанию трогать не нужно. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen(app: AppModel, openProfiles: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { TopAppBar(title = { Text(tr("Для опытных")) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Назад")) } }) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            Group(tr("Маршруты"), tr("Эти настройки меняют работу сети. Если не уверены, оставьте по умолчанию.")) {
                NavRow(tr("Профили маршрутизации"), routeProfileTitle(app), openProfiles)
                SwitchRow(tr("Все российские сайты напрямую"), if (app.allVpn) tr("В режиме «Весь трафик через VPN» не действует") else tr("Расширенно, поверх правил подписки"), app.ruDirect, !app.allVpn, app::chooseRuDirect)
            }
        }
    }
}
