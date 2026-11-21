package app.meows.vpn.ui.settings.pages

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.meows.vpn.i18n.tr
import androidx.activity.compose.rememberLauncherForActivityResult
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import app.meows.vpn.routing.RouteProfile

/** Отправить ссылку профиля через системное меню «Поделиться». */
fun shareProfile(ctx: Context, p: RouteProfile) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, p.toLink())
    runCatching { ctx.startActivity(Intent.createChooser(send, tr("Поделиться")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** QR из ссылки профиля: чёрное на белом при любой теме, низкая коррекция ошибок — чтобы длинный профиль поместился. null — не помещается. */
private fun qrBitmap(text: String, size: Int = 720): Bitmap? = runCatching {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 2))
    Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { b ->
        for (x in 0 until size) for (y in 0 until size) b.setPixel(x, y, if (m.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
    }
}.getOrNull()

@Composable
fun ProfileQrDialog(p: RouteProfile, onDismiss: () -> Unit) {
    val bmp = remember(p) { qrBitmap(p.toLink()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.name.ifBlank { tr("Профиль") }) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().size(280.dp))
                    Text(tr("Наведите камеру Meows, Happ или INCY на этот код, чтобы добавить профиль."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else Text(tr("Профиль слишком большой для QR-кода. Отправьте его через «Поделиться»."))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Закрыть")) } },
    )
}

/** Импорт профиля: ссылка `happ://routing/add/…`, base64 или JSON — вставить, ввести или отсканировать. [onImport] возвращает текст ошибки или null. */
@Composable
fun ProfileImportDialog(onImport: (String) -> String?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scan = rememberLauncherForActivityResult(ScanContract()) { r -> r.contents?.let { text = it.trim(); error = null } }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val clip = ctx.getSystemService(ClipboardManager::class.java).primaryClip
                        text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty(); error = null
                    }) { Text(tr("Вставить")) }
                    OutlinedButton(onClick = {
                        scan.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt(tr("Наведите камеру на QR-код профиля")).setBeepEnabled(false).setOrientationLocked(false))
                    }) { Text(tr("QR-код")) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { error = onImport(text); if (error == null) onDismiss() }) { Text(tr("Добавить")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена")) } },
    )
}
