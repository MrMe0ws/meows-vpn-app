package app.meows.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel
import app.meows.desktop.geo.GeoUpdater
import app.meows.desktop.i18n.tr
import app.meows.desktop.ui.components.hand
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Свои гео-файлы профиля: ссылки GeoIP/GeoSite, состояние файлов и кнопка «Обновить». Файлы в приложении общие,
 * поэтому скачивать можно только для выбранного профиля ([isSelected]). Пустые ссылки — из подписки, иначе вшитые.
 */
@Composable
internal fun GeoSection(app: AppModel, isSelected: Boolean, geoip: String, onGeoip: (String) -> Unit, geosite: String, onGeosite: (String) -> Unit) {
    app.profilesRev
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val status = app.geoStatus()
    val pins = GeoUpdater.pinsOf(geoip, geosite)
    val invalid = (geoip.isNotBlank() || geosite.isNotBlank()) && pins == null

    TextRow(tr("GeoIP URL"), geoip, { onGeoip(it); message = null }, "https://…/geoip.dat")
    TextRow(tr("GeoSite URL"), geosite, { onGeosite(it); message = null }, "https://…/geosite.dat")
    Text(
        tr("Гео-файлы используются для правил geoip: и geosite:. Если ссылки пустые, берутся ссылки из подписки, а если их нет — вшитые файлы."),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (invalid) Text(tr("Укажите обе ссылки, начинающиеся с https://"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(if (status.custom) Icons.Rounded.CheckCircle else Icons.Rounded.Info, null, Modifier.size(20.dp), tint = if (status.custom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (status.custom) tr("Гео-данные загружены") else tr("Используются файлы из приложения"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else OutlinedButton(
            onClick = {
                busy = true; message = null
                scope.launch {
                    val r = app.updateGeo(pins)
                    busy = false
                    message = when (r) {
                        GeoUpdater.Result.UPDATED -> tr("Гео-данные обновлены")
                        GeoUpdater.Result.BUNDLED -> tr("Возвращены файлы из приложения")
                        GeoUpdater.Result.UP_TO_DATE -> tr("Уже актуальны")
                        GeoUpdater.Result.FAILED -> tr("Не удалось скачать. Проверьте ссылки и сеть.")
                    }
                }
            },
            enabled = isSelected && !invalid, modifier = Modifier.hand(),
        ) { Text(tr("Обновить")) }
    }
    if (status.custom && status.updatedAt > 0) Text(
        tr("Обновлено: ") + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(status.updatedAt)),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
    if (!isSelected) Text(tr("Выберите этот профиль, чтобы скачать его гео-файлы."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
