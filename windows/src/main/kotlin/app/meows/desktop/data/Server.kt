package app.meows.desktop.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/** Сервер из подписки: флаг-эмодзи, название и строка протокола («VLESS · TCP · Reality · JSON»). */
data class Server(val title: String, val flag: String?, val subtitle: String)

object ServerParser {
    /** Тело подписки: Xray JSON (массив конфигов), либо base64 от него. */
    fun configs(body: String): JSONArray {
        var b = body.trim()
        if (b.isEmpty()) return JSONArray()
        if (b[0] != '[' && b[0] != '{') b = runCatching { String(Base64.getMimeDecoder().decode(b), Charsets.UTF_8).trim() }.getOrDefault("")
        return runCatching { if (b.startsWith("[")) JSONArray(b) else JSONArray().put(JSONObject(b)) }.getOrDefault(JSONArray())
    }

    fun servers(body: String): List<Server> {
        val arr = configs(body)
        return (0 until arr.length()).mapNotNull { i ->
            val cfg = arr.optJSONObject(i) ?: return@mapNotNull null
            val remarks = cfg.optString("remarks").ifBlank { "Сервер ${i + 1}" }
            val (flag, title) = splitFlag(remarks)
            Server(title.ifBlank { remarks }, flag, protoLine(cfg))
        }
    }

    /** Флаг — два региональных индикатора в начале названия (после невидимых символов). */
    private fun splitFlag(s: String): Pair<String?, String> {
        val cps = s.trimStart(' ', '\t', '\u200B', '\u200C', '\u200D', '\uFE0F', '\u2060', '\u00A0').codePoints().toArray()
        if (cps.size >= 2 && cps[0] in 0x1F1E6..0x1F1FF && cps[1] in 0x1F1E6..0x1F1FF) {
            val flag = String(cps, 0, 2)
            return flag to String(cps, 2, cps.size - 2).trim()
        }
        return null to s.trim()
    }

    private val names = mapOf(
        "vless" to "VLESS", "vmess" to "VMess", "trojan" to "Trojan", "shadowsocks" to "Shadowsocks", "hysteria" to "Hysteria2",
        "hysteria2" to "Hysteria2", "wireguard" to "WireGuard", "socks" to "SOCKS", "http" to "HTTP",
    )
    private val nets = mapOf(
        "tcp" to "TCP", "raw" to "TCP", "xhttp" to "XHTTP", "splithttp" to "XHTTP", "ws" to "WS", "grpc" to "gRPC",
        "httpupgrade" to "HTTPUpgrade", "kcp" to "mKCP", "quic" to "QUIC",
    )

    private fun protoLine(cfg: JSONObject): String {
        val outs = cfg.optJSONArray("outbounds") ?: return "—"
        for (i in 0 until outs.length()) {
            val o = outs.optJSONObject(i) ?: continue
            val raw = o.optString("protocol")
            if (raw.isEmpty() || raw in setOf("freedom", "blackhole", "dns", "loopback")) continue
            val p = names[raw] ?: raw.uppercase()
            val ss = o.optJSONObject("streamSettings")
            val sec = ss?.optString("security").orEmpty()
            val net = nets[ss?.optString("network").orEmpty()] ?: "TCP"
            return buildString {
                append(p)
                append(if (p == "Hysteria2" || p == "WireGuard") " · UDP" else " · $net")
                if (sec == "reality") append(" · Reality") else if (sec == "tls" || p == "Hysteria2") append(" · TLS")
                append(" · JSON")
            }
        }
        return "—"
    }
}