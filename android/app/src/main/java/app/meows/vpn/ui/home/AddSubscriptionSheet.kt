package app.meows.vpn.ui.home

import app.meows.vpn.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.vpn.Edition
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore

/** Ручной ввод ссылки подписки (нижний лист). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionSheet(vm: MeowsViewModel, replace: XKStore? = null, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(if (replace != null) tr("Заменить ссылку") else tr("Ссылка подписки"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                if (Edition.isPlay) tr("Вставьте ссылку подписки от вашего VPN-сервиса.") else tr("Вставьте ссылку из бота или личного кабинета Meows."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                url, { url = it }, Modifier.fillMaxWidth(),
                placeholder = { Text("https://…") }, singleLine = true,
                trailingIcon = { IconButton(onClick = { url = vm.clipboardText() }) { Icon(Icons.Rounded.ContentPaste, tr("Вставить")) } },
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(tr("Отмена")) }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = { if (replace != null) vm.replaceSubscription(replace, url) else vm.addSubscription(url); onDismiss() }, enabled = url.isNotBlank()) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(if (replace != null) tr("Заменить") else tr("Добавить"))
                }
            }
        }
    }
}
