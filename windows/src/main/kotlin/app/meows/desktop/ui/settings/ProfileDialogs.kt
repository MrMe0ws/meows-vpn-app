package app.meows.desktop.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import app.meows.desktop.i18n.tr
import app.meows.desktop.routing.RouteProfile
import app.meows.desktop.ui.components.hand
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage

internal fun copyToClipboard(text: String) = runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }

private fun clipboardText(): String = runCatching { Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String }.getOrDefault("")

/** QR из ссылки профиля: чёрное на белом при любой теме, низкая коррекция ошибок — чтобы длинный профиль поместился. null — не помещается. */
private fun qrImage(text: String, size: Int = 640): ImageBitmap? = runCatching {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 2))
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until size) for (y in 0 until size) img.setRGB(x, y, if (m.get(x, y)) 0x000000 else 0xFFFFFF)
    img.toComposeImageBitmap()
}.getOrNull()

@Composable
internal fun ProfileQrDialog(p: RouteProfile, onDismiss: () -> Unit) {
    val bmp = remember(p) { qrImage(p.toLink()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.name.ifBlank { tr("Профиль") }) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (bmp != null) {
                    Image(bmp, null, Modifier.size(300.dp))
                    Text(tr("Наведите камеру Meows, Happ или INCY на этот код, чтобы добавить профиль."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else Text(tr("Профиль слишком большой для QR-кода. Скопируйте ссылку из меню профиля."))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.hand()) { Text(tr("Закрыть")) } },
    )
}

/** Импорт профиля: ссылка `happ://routing/add/…`, base64 или JSON — вставить или ввести. [onImport] возвращает текст ошибки или null. */
@Composable
internal fun ProfileImportDialog(onImport: (String) -> String?, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Добавить профиль")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    text, { text = it; error = null }, Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
                    label = { Text(tr("Ссылка на профиль")) }, placeholder = { Text("happ://routing/add/…") },
                    isError = error != null, supportingText = { error?.let { Text(it) } },
                )
                OutlinedButton(onClick = { text = clipboardText().trim(); error = null }, modifier = Modifier.hand()) { Text(tr("Вставить")) }
            }
        },
        confirmButton = { TextButton(onClick = { error = onImport(text); if (error == null) onDismiss() }, modifier = Modifier.hand()) { Text(tr("Добавить")) } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.hand()) { Text(tr("Отмена")) } },
    )
}
