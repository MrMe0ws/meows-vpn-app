package app.meows.vpn.ui.home

import app.meows.vpn.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.shop.Diagnostics
import app.meows.vpn.shop.ShopLinks

/** «Поддержка»: отчёт с версией, устройством, сервером и последней ошибкой + переход в чат поддержки. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSheet(vm: MeowsViewModel, st: XKStore, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val report = remember { Diagnostics.build(ctx, st, vm.lastError) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(tr("Поддержка"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                tr("Мы скопируем отчёт о вашем устройстве — вставьте его в чат, так поддержка быстрее поймёт, что случилось. Ссылка подписки в отчёт не попадает."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(report, Modifier.fillMaxWidth().padding(16.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        vm.copy(report, "support"); vm.toast = tr("Отчёт скопирован — вставьте его в чат")
                        vm.openLink(ShopLinks.support(st)); onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Rounded.Send, null); Spacer(Modifier.width(8.dp)); Text(tr("Написать в поддержку")) }
                FilledTonalButton(
                    onClick = { vm.copy(report, "support"); vm.toast = tr("Отчёт скопирован"); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text(tr("Только скопировать отчёт")) }
            }
        }
    }
}
