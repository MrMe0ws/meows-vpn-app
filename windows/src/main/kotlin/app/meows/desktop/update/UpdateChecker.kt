package app.meows.desktop.update

import app.meows.desktop.i18n.trf
import app.meows.desktop.i18n.tr
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Ищет последний релиз Windows на GitHub: тег `windows-vX.Y.Z` и ассет `.msi` (репозиторий общий с Android, у него теги `android-v…`). */
object UpdateChecker {
    const val OWNER = "MrMe0ws"
    const val REPO = "meows-vpn-app"
    const val TAG_PREFIX = "windows-v"
    const val RELEASES_URL = "https://github.com/$OWNER/$REPO/releases"

    sealed interface Result {
        data class Found(val info: ReleaseInfo) : Result
        data object NoReleases : Result
        data class Failed(val message: String) : Result
    }

    fun latest(): Result = try {
        val c = URL("https://api.github.com/repos/$OWNER/$REPO/releases?per_page=20").openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/vnd.github+json"); c.setRequestProperty("User-Agent", "Meows-Windows")
        try {
            when (val code = c.responseCode) {
                404 -> Result.NoReleases
                403, 429 -> Result.Failed(tr("GitHub временно ограничил запросы. Попробуйте позже."))
                in 200..299 -> parse(c.inputStream.bufferedReader().use { it.readText() })
                else -> Result.Failed(trf("GitHub ответил %d", code))
            }
        } finally { c.disconnect() }
    } catch (e: Exception) { Result.Failed(trf("Нет связи с GitHub: %s", e.message)) }

    private fun parse(json: String): Result {
        val list = JSONArray(json)
        var sawWindows = false
        for (i in 0 until list.length()) {
            val o = list.getJSONObject(i)
            val tag = o.optString("tag_name")
            if (o.optBoolean("draft") || o.optBoolean("prerelease") || !tag.startsWith(TAG_PREFIX)) continue
            sawWindows = true
            val assets = o.optJSONArray("assets") ?: continue
            for (k in 0 until assets.length()) {
                val a = assets.getJSONObject(k)
                if (a.optString("name").endsWith(".msi", ignoreCase = true))
                    return Result.Found(ReleaseInfo(tag.removePrefix(TAG_PREFIX), o.optString("body"), a.getString("browser_download_url"), a.optLong("size")))
            }
        }
        return if (sawWindows) Result.Failed(tr("В последнем релизе нет установщика (.msi).")) else Result.NoReleases
    }
}