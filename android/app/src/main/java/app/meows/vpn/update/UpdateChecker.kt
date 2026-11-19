package app.meows.vpn.update

import app.meows.vpn.i18n.trf
import app.meows.vpn.i18n.tr
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Спрашивает у GitHub последний Android-релиз. Вызывать вне главного потока. */
object UpdateChecker {
    sealed interface Result {
        data class Found(val info: ReleaseInfo) : Result
        data object NoReleases : Result
        data class Failed(val message: String) : Result
    }

    fun latest(): Result = try {
        val c = URL(AppLinks.RELEASES_API).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "MeowsVPN-Android")
        when (val code = c.responseCode) {
            404 -> Result.NoReleases
            in 200..299 -> parse(c.inputStream.bufferedReader().use { it.readText() })
            403, 429 -> Result.Failed(tr("GitHub временно ограничил запросы. Попробуйте позже."))
            else -> Result.Failed(trf("GitHub ответил %d", code))
        }.also { c.disconnect() }
    } catch (e: Exception) {
        Result.Failed(trf("Нет связи с GitHub: %s", e.message ?: e.javaClass.simpleName))
    }

    /** Берёт самый свежий не-черновик с тегом `android-vX.Y.Z` и APK в ассетах (GitHub отдаёт список от новых к старым). */
    private fun parse(json: String): Result {
        val list = JSONArray(json)
        var sawAndroid = false
        for (i in 0 until list.length()) {
            val o = list.getJSONObject(i)
            val tag = o.optString("tag_name")
            if (o.optBoolean("draft") || o.optBoolean("prerelease") || !tag.startsWith(AppLinks.ANDROID_TAG_PREFIX)) continue
            sawAndroid = true
            val assets = o.optJSONArray("assets") ?: continue
            for (k in 0 until assets.length()) {
                val a = assets.getJSONObject(k)
                if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                    return Result.Found(
                        ReleaseInfo(
                            version = tag.removePrefix(AppLinks.ANDROID_TAG_PREFIX),
                            notes = o.optString("body"),
                            apkUrl = a.getString("browser_download_url"),
                            apkSize = a.optLong("size"),
                            pageUrl = o.optString("html_url", AppLinks.RELEASES_URL),
                        ),
                    )
                }
            }
        }
        return if (sawAndroid) Result.Failed(tr("В последнем релизе нет APK-файла.")) else Result.NoReleases
    }
}