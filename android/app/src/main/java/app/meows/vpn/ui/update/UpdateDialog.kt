package app.meows.vpn.ui.update

import app.meows.vpn.i18n.trf
import app.meows.vpn.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.ui.components.formatBytesRu
import app.meows.vpn.update.UpdateState

/**
 * «Доступна новая версия»: шторка почти на весь экран (канон Material для длинного содержимого) с полным списком изменений.
 * Внизу две кнопки: главная залитая («Скачать» → «Установить») и «Позже» (закрывает; автопроверка предложит снова
 * при одном из следующих запусков, не чаще раза в 12 часов). Пока идёт загрузка, шторку закрыть нельзя.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(vm: MeowsViewModel) {
    val u = vm.updates
    if (!u.dialogVisible) return
    val s = u.state
    val info = when (s) {
        is UpdateState.Available -> s.info
        is UpdateState.Downloading -> s.info
        is UpdateState.Downloaded -> s.info
        else -> return
    }
    val busy = s is UpdateState.Downloading
    val busyNow = rememberUpdatedState(busy)

    ModalBottomSheet(
        onDismissRequest = { if (!busy) u.dismissDialog() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !busyNow.value || it != SheetValue.Hidden }),
    ) {
        Column(Modifier.fillMaxHeight().padding(horizontal = 24.dp)) {
            Text(trf("Доступна версия %s", info.version), style = MaterialTheme.typography.headlineSmall)
            Text(
                trf("Сейчас установлена %s", u.currentVersion) + if (info.apkSize > 0) " · ${formatBytesRu(info.apkSize)}" else "",
                Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (info.notes.isBlank()) Text(tr("Описание изменений не указано."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else ReleaseNotesText(info.notes)
            }
            if (s is UpdateState.Downloading) {
                Spacer(Modifier.height(12.dp))
                Text(trf("Скачиваем… %d%%", s.percent), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 12.dp, bottom = 16.dp), horizontalArrangement = Arrangement.End) {
                if (!busy) TextButton(onClick = u::dismissDialog) { Text(tr("Позже")) }
                Spacer(Modifier.width(8.dp))
                when (s) {
                    is UpdateState.Downloaded -> Button(onClick = {
                        if (u.install()) u.dismissDialog()
                        else vm.toast = tr("Разрешите установку из этого источника и нажмите «Установить» ещё раз")
                    }) { Text(tr("Установить")) }
                    is UpdateState.Available -> Button(onClick = u::download) { Text(tr("Скачать")) }
                    else -> {}
                }
            }
        }
    }
}
