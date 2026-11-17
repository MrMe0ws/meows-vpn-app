package app.meows.desktop.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/** Загруженная подписка: тело (Xray JSON), заголовки панели и время обновления. */
class SubscriptionData(val url: String, val body: String, val headers: Map<String, String>, val updated: Long) {
    val servers: List<Server> by lazy { ServerParser.servers(body) }

    private fun header(n: String) = headers[n]?.takeIf { it.isNotBlank() }
    private fun decoded(v: String?) = if (v != null && v.startsWith("base64:"))
        runCatching { String(Base64.getMimeDecoder().decode(v.substring(7)), Charsets.UTF_8) }.getOrNull() else v

    val title: String get() = decoded(header("profile-title")) ?: "Meows"
    val announce: String? get() = decoded(header("announce"))
    val supportUrl: String? get() = header("support-url")
    val webUrl: String? get() = header("profile-web-page-url")
    val updateIntervalHours: Int get() = header("profile-update-interval")?.trim()?.toIntOrNull() ?: 0

    private fun info(key: String): Long = header("subscription-userinfo")?.split(";")?.map { it.trim().split("=") }
        ?.firstOrNull { it.size == 2 && it[0] == key }?.get(1)?.toLongOrNull() ?: 0
    val used: Long get() = info("upload") + info("download")
    val total: Long get() = info("total")
    val expireSec: Long get() = info("expire")

    fun toJson() = JSONObject().put("url", url).put("body", body).put("headers", JSONObject(headers)).put("updated", updated)

    companion object {
        fun fromJson(s: String): SubscriptionData? = runCatching {
            val j = JSONObject(s)
            val h = j.getJSONObject("headers")
            SubscriptionData(j.getString("url"), j.getString("body"), h.keys().asSequence().associateWith { h.getString(it) }, j.getLong("updated"))
        }.getOrNull()
    }
}

/** Загрузка подписки. Сначала представляемся как MeowsVPN; если панель отдала ссылки вместо JSON — повторяем как Happ. */
object SubscriptionClient {
    fun fetch(raw: String, version: String): Result<SubscriptionData> {
        var link = raw.trim()
        for (pre in listOf("happ://add/", "meows://add/")) if (link.startsWith(pre)) link = link.removePrefix(pre)
        if (!(link.startsWith("https://") || link.startsWith("http://"))) return Result.failure(Exception("Это не похоже на ссылку подписки."))
        var last: Exception? = null
        for (ua in listOf("MeowsVPN/$version", "Happ/3.9.0")) {
            val r = runCatching { request(link, ua) }
            if (r.isSuccess) return r
            last = r.exceptionOrNull() as? Exception
            if (last?.message?.startsWith("Сервер отдал ссылки") != true) break
        }
        return Result.failure(last ?: Exception("Не получилось загрузить подписку."))
    }

    private fun request(link: String, ua: String): SubscriptionData {
        val c = URL(link).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000; c.readTimeout = 20_000
            c.setRequestProperty("User-Agent", ua)
            c.setRequestProperty("Accept", "*/*")
            c.setRequestProperty("x-hwid", Hwid.value)
            c.setRequestProperty("x-device-os", "Windows")
            c.setRequestProperty("x-ver-os", System.getProperty("os.version"))
            c.setRequestProperty("x-device-model", System.getenv("COMPUTERNAME") ?: "PC")
            c.setRequestProperty("x-app-name", "MeowsVPN")
            val code = try { c.responseCode } catch (e: Exception) { throw Exception("Нет связи с сервером подписки.\n${e.message}") }
            if (code >= 400) throw Exception(
                "Сервер ответил $code. " + if (code == 404) "Подписка не найдена — проверьте ссылку."
                else "Возможно, достигнут лимит устройств. Удалите старое устройство в боте или кабинете.",
            )
            val body = c.inputStream.use { String(it.readBytes(), Charsets.UTF_8) }
            val t = body.trim()
            if (t.isEmpty()) throw Exception("Подписка пустая. Возможно, превышен лимит устройств.")
            if (t[0] != '[' && t[0] != '{') {
                val dec = runCatching { String(Base64.getMimeDecoder().decode(t), Charsets.UTF_8).trim() }.getOrDefault("")
                if (!(dec.startsWith("[") || dec.startsWith("{")))
                    throw Exception("Сервер отдал ссылки вместо Xray JSON. Включите Xray JSON шаблон для клиентов Meows в Remnawave.")
            }
            val headers = HashMap<String, String>()
            for ((k, v) in c.headerFields) if (k != null && !v.isNullOrEmpty()) headers[k.lowercase()] = v[0]
            return SubscriptionData(link, body, headers, System.currentTimeMillis())
        } finally { c.disconnect() }
    }
}