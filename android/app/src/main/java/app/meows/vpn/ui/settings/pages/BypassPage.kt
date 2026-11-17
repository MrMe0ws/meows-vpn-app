package app.meows.vpn.ui.settings.pages

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow

/** Категории и свои домены, которые идут мимо VPN. Применяется при выходе со страницы. */
@Composable
fun BypassPage(vm: MeowsViewModel, onBack: () -> Unit) {
    val cats = remember { mutableStateListOf<String>().apply { addAll(vm.st.bypassCats()) } }
    val sites = remember { mutableStateListOf<String>().apply { addAll(vm.st.bypassSites()) } }
    var input by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }

    val vmNow by rememberUpdatedState(vm)
    DisposableEffect(Unit) {
        onDispose {
            if (cats.toSet() != vmNow.st.bypassCats()) vmNow.setBypassCats(cats.toSet())
            if (sites.toSet() != vmNow.st.bypassSites().toSet()) vmNow.setBypassSites(sites.toList())
        }
    }

    SettingsPageScaffold("Мимо VPN", onBack) {
        SettingsGroup("Категории", "Включённые категории работают через обычный интернет, без VPN.") {
            XKStore.CATEGORIES.forEach { c ->
                SwitchRow(Icons.Rounded.Language, c[1], c[0] in cats, c[2]) { on -> if (on) cats.add(c[0]) else cats.remove(c[0]) }
            }
        }
        SettingsGroup("Свои сайты", "Домен вместе с поддоменами пойдёт мимо VPN. Например: example.com") {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    input, { input = it; inputError = false }, Modifier.weight(1f),
                    singleLine = true, placeholder = { Text("example.com") }, isError = inputError,
                    supportingText = if (inputError) { { Text("Не похоже на домен") } } else null,
                )
                IconButton(onClick = {
                    val d = XKStore.normalizeSite(input)
                    if (d.isEmpty()) inputError = true
                    else { if (d !in sites) sites.add(d); sites.sort(); input = "" }
                }) { Icon(Icons.Rounded.Add, "Добавить") }
            }
            sites.toList().forEach { s ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(s) },
                    trailingContent = { IconButton(onClick = { sites.remove(s) }) { Icon(Icons.Rounded.Close, "Удалить") } },
                )
            }
        }
    }
}
