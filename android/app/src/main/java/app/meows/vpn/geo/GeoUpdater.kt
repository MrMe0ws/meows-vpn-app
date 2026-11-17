package app.meows.vpn.geo

import android.content.Context
import android.util.Base64
import app.meows.vpn.XKStore
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Гео-файлы для маршрутизации следуют за панелью, как в Happ: в заголовке `routing` ответа подписки лежат
 * закреплённые ссылки (`Geoipurl`, `Geositeurl`). Вшитые в приложение файлы соответствуют закреплению на момент релиза;
 * если админ в панели поменял ссылки, скачиваем новые файлы. Применяются при следующем подключении VPN.
 */
object GeoUpdater {
    private const val BUNDLED_SITE = "https://cdn.jsdelivr.net/gh/hydraponique/roscomvpn-geosite@202604152235/release/geosite.dat"
    private const val BUNDLED_IP = "https://cdn.jsdelivr.net/gh/hydraponique/roscomvpn-geoip@202605210659/release/geoip.dat"
    private const val KEY_SITE = "geo.site.url"
    private const val KEY_IP = "geo.ip.url"

    data class Pins(val geoip: String, val geosite: String)

    /** `happ://routing/add/<base64 JSON>` (у INCY без схемы) → ссылки на geo-файлы. */
    fun parsePins(header: String?): Pins? {
        if (header.isNullOrBlank()) return null
        return runCatching {
            val b64 = header.trim().substringAfterLast('/')
            val json = JSONObject(String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8))
            val ip = json.optString("Geoipurl"); val site = json.optString("Geositeurl")
            if (ip.startsWith("https://") && site.startsWith("https://")) Pins(ip, site) else null
        }.getOrNull()
    }

    /** Блокирующий вызов (вне главного потока). Ошибки сети молча игнорируются: остаются прежние файлы. */
    fun sync(ctx: Context, st: XKStore) {
        val pins = parsePins(st.routingHeader()) ?: return
        val prefs = st.prefs()
        val curSite = prefs.getString(KEY_SITE, BUNDLED_SITE)
        val curIp = prefs.getString(KEY_IP, BUNDLED_IP)
        if (pins.geosite == curSite && pins.geoip == curIp) return

        val dir = File(ctx.filesDir, "geo").apply { mkdirs() }
        val site = download(pins.geosite, File(dir, "geosite.dat.tmp")) ?: return
        val ip = download(pins.geoip, File(dir, "geoip.dat.tmp")) ?: run { site.delete(); return }
        site.renameTo(File(dir, "geosite.dat"))
        ip.renameTo(File(dir, "geoip.dat"))
        File(dir, "custom").writeText("1")
        prefs.edit().putString(KEY_SITE, pins.geosite).putString(KEY_IP, pins.geoip).apply()
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