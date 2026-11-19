package app.meows.vpn.ui.home

import app.meows.vpn.i18n.tr
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore

/** JSON-конфигурация сервера (долгое нажатие на строку). */
@Composable
fun ConfigDialog(vm: MeowsViewModel, st: XKStore, index: Int, onDismiss: () -> Unit) {
    val cfg = remember(index) { st.prettyConfig(index) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(st.servers().getOrNull(index)?.title ?: tr("Конфигурация")) },
        text = {
            Column(Modifier.height(320.dp).verticalScroll(rememberScrollState())) {
                Text(
                    cfg, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Закрыть")) } },
        dismissButton = { TextButton(onClick = { vm.copy(cfg, "config"); vm.toast = tr("Скопировано") }) { Text(tr("Копировать")) } },
    )
}
