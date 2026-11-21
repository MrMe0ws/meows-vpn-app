package app.meows.desktop.geo

import app.meows.desktop.data.AppDirs
import app.meows.desktop.data.SettingsStore
import app.meows.desktop.routing.RoutingProfiles
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

/**
 * Гео-файлы для маршрутизации (как на Android: `android/.../geo/GeoUpdater.kt`). Какие ссылки действуют: ссылки выбранного профиля
 * (`Geoipurl`/`Geositeurl`) → закреплённые панелью в заголовке `routing` подписки → вшитые файлы. Скачанные файлы лежат в
 * %APPDATA%\Meows\geo с маркером `custom`; без маркера `CoreProcess` держит там вшитые. Применяются при следующем подключении.
 */
object GeoUpdater {
    private const val BUNDLED_SITE = "https://cdn.jsdelivr.net/gh/hydraponique/roscomvpn-geosite@202604152235/release/geosite.dat"
    private const val BUNDLED_IP = "https://cdn.jsdelivr.net/gh/hydraponique/roscomvpn-geoip@202605210659/release/geoip.dat"
    private const val KEY_SITE = "geo.site.url"
    private const val KEY_IP = "geo.ip.url"
    private const val KEY_TIME = "geo.time"

    data class Pins(val geoip: String, val geosite: String)
    enum class Result { UPDATED, UP_TO_DATE, BUNDLED, FAILED }
    data class Status(val custom: Boolean, val updatedAt: Long)

    /** `happ://routing/add/<base64 JSON>` → ссылки на geo-файлы. */
    fun parsePins(header: String?): Pins? {
        if (header.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(String(Base64.getMimeDecoder().decode(header.trim().substringAfterLast('/')), Charsets.UTF_8))
            pinsOf(json.optString("Geoipurl"), json.optString("Geositeurl"))
        }.getOrNull()
    }

    /** Обе ссылки должны быть https; иначе «своих гео-ссылок нет». */
    fun pinsOf(geoip: String, geosite: String): Pins? {
        val ip = geoip.trim(); val site = geosite.trim()
        return if (ip.startsWith("https://") && site.startsWith("https://")) Pins(ip, site) else null
    }

    /** Ссылки, которые должны действовать сейчас: профиль → подписка → null (вшитые). */
    fun wanted(st: SettingsStore, header: String?): Pins? =
        RoutingProfiles.selectedProfile(st)?.let { pinsOf(it.geoipUrl, it.geositeUrl) } ?: parsePins(header)

    fun status(st: SettingsStore) = Status(File(AppDirs.geo, "custom").exists(), st.long(KEY_TIME))

    /** Блокирующий вызов (вне UI-потока). Ошибки сети не трогают прежние файлы. [force] — скачать, даже если ссылки те же. */
    fun sync(st: SettingsStore, pins: Pins?, force: Boolean = false): Result {
        val dir = AppDirs.geo
        val marker = File(dir, "custom")
        if (pins == null) {
            // ссылок нет ни у профиля, ни у подписки: возвращаем вшитые файлы (CoreProcess скопирует их при подключении)
            if (!marker.exists()) return Result.UP_TO_DATE
            marker.delete()
            st.set(KEY_SITE, BUNDLED_SITE); st.set(KEY_IP, BUNDLED_IP); st.set(KEY_TIME, null)
            return Result.BUNDLED
        }
        val custom = marker.exists()
        val downloadedSame = custom && pins.geosite == st.str(KEY_SITE, BUNDLED_SITE) && pins.geoip == st.str(KEY_IP, BUNDLED_IP)
        val bundledSame = !custom && pins.geosite == BUNDLED_SITE && pins.geoip == BUNDLED_IP
        if (!force && (downloadedSame || bundledSame)) return Result.UP_TO_DATE

        val site = download(pins.geosite, File(dir, "geosite.dat.tmp")) ?: return Result.FAILED
        val ip = download(pins.geoip, File(dir, "geoip.dat.tmp")) ?: run { site.delete(); return Result.FAILED }
        site.copyTo(File(dir, "geosite.dat"), overwrite = true); site.delete()
        ip.copyTo(File(dir, "geoip.dat"), overwrite = true); ip.delete()
        marker.writeText("1")
        st.set(KEY_SITE, pins.geosite); st.set(KEY_IP, pins.geoip); st.set(KEY_TIME, System.currentTimeMillis())
        return Result.UPDATED
    }

    private fun download(url: String, out: File): File? = runCatching {
        val c = URI(url).toURL().openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "Meows-Windows")
        try {
            if (c.responseCode !in 200..299) return null
            c.inputStream.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
        } finally { c.disconnect() }
        // sanity: не пустой и не гигантский файл
        if (out.length() < 1_000 || out.length() > 60_000_000) { out.delete(); null } else out
    }.getOrNull()
}
