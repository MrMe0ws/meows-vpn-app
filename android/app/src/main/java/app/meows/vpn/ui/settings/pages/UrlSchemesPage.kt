package app.meows.vpn.ui.settings.pages

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.i18n.tr
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold

/** «Схемы URL-адресов»: какие ссылки `meows://…` понимает приложение (для разработчиков сайтов, ботов и кабинетов). Нажатие копирует шаблон. */
@Composable
fun UrlSchemesPage(vm: MeowsViewModel, onBack: () -> Unit) {
    SettingsPageScaffold(tr("Схемы URL-адресов"), onBack) {
        SettingsGroup(
            tr("Команда"),
            footer = tr("Нажмите на команду, чтобы скопировать шаблон. {url} — ссылка подписки целиком (https://…), можно и в URL-кодировке."),
        ) {
            SchemeRow("meows://add/{url}", tr("Добавить подписку. Если такая ссылка уже есть, она обновится.")) { vm.copy(it); vm.toast = tr("Скопировано") }
        }
        SettingsGroup(
            tr("Пример"),
            footer = tr("Кнопку со ссылкой можно поставить на сайт, в бот или кабинет: приложение откроется само и загрузит подписку. Других команд пока нет."),
        ) {
            SchemeRow("meows://add/https://sub.example.com/abc123", tr("Откроет Meows и добавит эту подписку.")) { vm.copy(it); vm.toast = tr("Скопировано") }
        }
    }
}

@Composable
private fun SchemeRow(scheme: String, description: String, onCopy: (String) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onCopy(scheme) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(scheme, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary) },
        supportingContent = { Text(description) },
    )
}
