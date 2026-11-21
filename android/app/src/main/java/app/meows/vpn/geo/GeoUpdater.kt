package app.meows.vpn.geo

import android.content.Context
import android.util.Base64
import app.meows.vpn.XKStore
import app.meows.vpn.routing.RoutingProfiles
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Гео-файлы для маршрутизации. Какие ссылки действуют, решается так: ссылки выбранного профиля маршрутизации
 * (`Geoipurl`/`Geositeurl`) → закреплённые панелью в заголовке `routing` подписки (как в Happ) → вшитые в приложение файлы.
 * Скачанные файлы лежат в `files/geo/` с маркером `custom`; без маркера `XKVpnService` держит там вшитые.
 * Новые файлы применяются при следующем подключении VPN.
 */
object GeoUpdater {
    private const val BUNDLED_SITE = "https://cdn.jsdelivr.net/gh/hydraponique/roscomvpn-geosite@202604152235/release/geosite.dat"
    private const val BUNDLED_IP = "https://cdn.jsdelivr.net/gh/hydraponique/roscomvpn-geoip@202605210659/release/geoip.dat"
    private const val KEY_SITE = "geo.site.url"
    private const val KEY_IP = "geo.ip.url"
    private const val KEY_TIME = "geo.time"

    data class Pins(val geoip: String, val geosite: String)

    /** Что случилось при синхронизации. */
    enum class Result { UPDATED, UP_TO_DATE, BUNDLED, FAILED }

    /** Состояние файлов для экрана: скачаны ли свои, когда. */
    data class Status(val custom: Boolean, val updatedAt: Long)

    /** `happ://routing/add/<base64 JSON>` (у INCY без схемы) → ссылки на geo-файлы. */
    fun parsePins(header: String?): Pins? {
        if (header.isNullOrBlank()) return null
        return runCatching {
            val b64 = header.trim().substringAfterLast('/')
            val json = JSONObject(String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8))
            pinsOf(json.optString("Geoipurl"), json.optString("Geositeurl"))
        }.getOrNull()
    }

    /** Обе ссылки должны быть https; иначе профиль «без своих гео-ссылок». */
    fun pinsOf(geoip: String, geosite: String): Pins? {
        val ip = geoip.trim(); val site = geosite.trim()
        return if (ip.startsWith("https://") && site.startsWith("https://")) Pins(ip, site) else null
    }

    /** Ссылки, которые должны действовать сейчас: профиль → подписка → null (вшитые). */
    fun wanted(st: XKStore): Pins? =
        RoutingProfiles.selectedProfile(st)?.let { pinsOf(it.geoipUrl, it.geositeUrl) } ?: parsePins(st.routingHeader())

    fun status(ctx: Context, st: XKStore): Status =
        Status(File(ctx.filesDir, "geo/custom").exists(), st.prefs().getLong(KEY_TIME, 0))

    /** Блокирующий вызов (вне главного потока). Ошибки сети не трогают прежние файлы. [force] — скачать, даже если ссылки те же. */
    fun sync(ctx: Context, st: XKStore, pins: Pins? = wanted(st), force: Boolean = false): Result {
        val prefs = st.prefs()
        val dir = File(ctx.filesDir, "geo").apply { mkdirs() }
        if (pins == null) {
            // ссылок нет ни у профиля, ни у подписки: возвращаем вшитые файлы (сервис скопирует их при подключении)
            if (!File(dir, "custom").exists()) return Result.UP_TO_DATE
            File(dir, "custom").delete()
            prefs.edit().putString(KEY_SITE, BUNDLED_SITE).putString(KEY_IP, BUNDLED_IP).remove(KEY_TIME).apply()
            return Result.BUNDLED
        }
        val custom = File(dir, "custom").exists()
        // уже стоят нужные файлы: скачанные по этим же ссылкам, либо вшитые, если ссылки совпали с вшитыми
        val downloadedSame = custom && pins.geosite == prefs.getString(KEY_SITE, BUNDLED_SITE) && pins.geoip == prefs.getString(KEY_IP, BUNDLED_IP)
        val bundledSame = !custom && pins.geosite == BUNDLED_SITE && pins.geoip == BUNDLED_IP
        if (!force && (downloadedSame || bundledSame)) return Result.UP_TO_DATE

        val site = download(pins.geosite, File(dir, "geosite.dat.tmp")) ?: return Result.FAILED
        val ip = download(pins.geoip, File(dir, "geoip.dat.tmp")) ?: run { site.delete(); return Result.FAILED }
        site.renameTo(File(dir, "geosite.dat"))
        ip.renameTo(File(dir, "geoip.dat"))
        File(dir, "custom").writeText("1")
        prefs.edit().putString(KEY_SITE, pins.geosite).putString(KEY_IP, pins.geoip).putLong(KEY_TIME, System.currentTimeMillis()).apply()
        return Result.UPDATED
    }

    private fun download(url: String, out: File): File? = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "MeowsVPN-Android")
        try {
            if (c.responseCode !in 200..299) return null
            c.inputStream.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
        } finally { c.disconnect() }
        // sanity: не пустой и не гигантский файл
        if (out.length() < 1_000 || out.length() > 60_000_000) { out.delete(); null } else out
    }.getOrNull()
}
