package app.meows.vpn.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Скачивает APK в кэш и отдаёт его системному установщику. */
object UpdateInstaller {
    private fun dir(ctx: Context) = File(ctx.cacheDir, "updates").apply { mkdirs() }

    /** Блокирующая загрузка; [progress] получает 0..100. Бросает исключение при ошибке. */
    fun download(ctx: Context, info: ReleaseInfo, progress: (Int) -> Unit): File {
        val dir = dir(ctx)
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "MeowsVPN-${info.version}.apk")
        val c = URL(info.apkUrl).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "MeowsVPN-Android")
        try {
            if (c.responseCode !in 200..299) error("Сервер ответил ${c.responseCode}")
            val total = c.contentLengthLong.takeIf { it > 0 } ?: info.apkSize
            var done = 0L
            c.inputStream.use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    var n: Int
                    var last = -1
                    while (input.read(buf).also { n = it } > 0) {
                        o.write(buf, 0, n); done += n
                        if (total > 0) {
                            val p = (done * 100 / total).toInt()
                            if (p != last) { last = p; progress(p) }
                        }
                    }
                }
            }
            if (total > 0 && done != total) { out.delete(); error("Файл скачан не полностью") }
        } finally { c.disconnect() }
        return out
    }

    /** Нужно ли сначала выдать разрешение «Установка из неизвестных источников». */
    fun needsInstallPermission(ctx: Context) = !ctx.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
