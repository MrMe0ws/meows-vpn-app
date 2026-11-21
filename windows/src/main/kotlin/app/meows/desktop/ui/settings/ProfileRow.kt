package app.meows.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.desktop.i18n.tr
import app.meows.desktop.ui.components.IconButton
import app.meows.desktop.ui.components.hand

/** Что можно сделать с профилем из меню ⋮. Пункт показывается, только если действие задано: «Удалить» — у своих, «Сбросить» — у изменённых встроенных. */
internal class ProfileActions(
    val onCopy: () -> Unit, val onView: (() -> Unit)? = null, val onLink: (() -> Unit)? = null, val onQr: (() -> Unit)? = null,
    val onEdit: (() -> Unit)? = null, val onReset: (() -> Unit)? = null, val onDelete: (() -> Unit)? = null,
)

/** Карточка профиля: радио, название, описание и метки (серая — вторая), справа меню ⋮. */
@Composable
internal fun ProfileRow(title: String, desc: String, chips: List<Pair<String, Boolean>>, selected: Boolean, actions: ProfileActions, onSelect: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onSelect).hand(),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { RadioButton(selected, onClick = null) },
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text(desc)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { chips.forEach { (text, accent) -> ProfileChip(text, accent) } }
            }
        },
        trailingContent = { ProfileMenu(actions) },
    )
}

@Composable
private fun ProfileMenu(a: ProfileActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, tr("Действия")) }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            a.onView?.let { f -> DropdownMenuItem({ Text(tr("Посмотреть")) }, { open = false; f() }, Modifier.hand(), leadingIcon = { Icon(Icons.Rounded.Visibility, null) }) }
            a.onEdit?.let { f -> DropdownMenuItem({ Text(tr("Изменить")) }, { open = false; f() }, Modifier.hand(), leadingIcon = { Icon(Icons.Rounded.Edit, null) }) }
            DropdownMenuItem({ Text(tr("Копировать")) }, { open = false; a.onCopy() }, Modifier.hand(), leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) })
            a.onLink?.let { f -> DropdownMenuItem({ Text(tr("Скопировать ссылку")) }, { open = false; f() }, Modifier.hand(), leadingIcon = { Icon(Icons.Rounded.Link, null) }) }
            a.onQr?.let { f -> DropdownMenuItem({ Text(tr("QR")) }, { open = false; f() }, Modifier.hand(), leadingIcon = { Icon(Icons.Rounded.QrCode2, null) }) }
            a.onReset?.let { f -> DropdownMenuItem({ Text(tr("Сбросить к исходному")) }, { open = false; f() }, Modifier.hand(), leadingIcon = { Icon(Icons.Rounded.RestartAlt, null) }) }
            a.onDelete?.let { f -> DropdownMenuItem({ Text(tr("Удалить"), color = MaterialTheme.colorScheme.error) }, { open = false; f() }, Modifier.hand(), leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }) }
        }
    }
}

@Composable
private fun ProfileChip(text: String, accent: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = if (accent) scheme.secondaryContainer else scheme.surfaceContainerHighest,
        contentColor = if (accent) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) { Text(text, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
}
