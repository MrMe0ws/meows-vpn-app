package app.meows.desktop.routing

import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Правила шаблона подписки живут в Xray-конфиге каждого сервера (`routing.rules`), а не в заголовке `routing` (там у панели
 * обычно имя, DNS и гео-ссылки, а списки пусты). Здесь они переводятся в списки профиля, чтобы «Из подписки» можно было
 * посмотреть и скопировать. Соответствие: цель правила → группа (freedom → «напрямую», blackhole → «блок», остальное и балансировщик → «через VPN»).
 * Правила без доменов и IP (порты, протоколы, DNS) в профиль не переходят. Порядок групп берётся по последнему правилу каждой группы:
 * широкие «напрямую» стоят в шаблонах в конце, а узкие исключения («через VPN») выше.
 */
object XrayRules {
    /** Конфиг сервера [index] из тела подписки (Xray JSON: объект, массив или base64 от них); null — не разобрать. */
    fun config(body: String, index: Int): JSONObject? = runCatching {
        var t = body.trim()
        if (t.isNotEmpty() && t[0] != '{' && t[0] != '[') t = String(Base64.getMimeDecoder().decode(t), Charsets.UTF_8).trim()
        if (t.startsWith("[")) JSONArray(t).let { a -> a.optJSONObject(index.coerceIn(0, (a.length() - 1).coerceAtLeast(0))) } else JSONObject(t)
    }.getOrNull()

    private fun kindOf(tag: String, outbounds: JSONArray?): String? {
        for (i in 0 until (outbounds?.length() ?: 0)) {
            val o = outbounds!!.optJSONObject(i) ?: continue
            if (o.optString("tag") != tag) continue
            return when (o.optString("protocol")) { "freedom" -> "direct"; "blackhole" -> "block"; "dns", "loopback" -> null; else -> "proxy" }
        }
        return when (tag.lowercase()) { "direct" -> "direct"; "block" -> "block"; "proxy" -> "proxy"; else -> null }
    }

    private fun strings(o: JSONObject, key: String): List<String> =
        o.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotEmpty() } } ?: emptyList()

    /** Правила [cfg] поверх [base] (имя, DNS, гео-ссылки — из заголовка панели). null — в конфиге нет правил с доменами или IP. */
    fun profileFrom(cfg: JSONObject, base: RouteProfile): RouteProfile? {
        val routing = cfg.optJSONObject("routing") ?: return null
        val rules = routing.optJSONArray("rules") ?: return null
        val sites = linkedMapOf("block" to linkedSetOf<String>(), "proxy" to linkedSetOf(), "direct" to linkedSetOf())
        val ips = linkedMapOf("block" to linkedSetOf<String>(), "proxy" to linkedSetOf(), "direct" to linkedSetOf())
        val last = HashMap<String, Int>()
        for (i in 0 until rules.length()) {
            val r = rules.optJSONObject(i) ?: continue
            val kind = if (r.has("balancerTag")) "proxy" else kindOf(r.optString("outboundTag"), cfg.optJSONArray("outbounds")) ?: continue
            if (strings(r, "inboundTag").any { it.equals("api", true) }) continue
            val d = strings(r, "domain"); val ip = strings(r, "ip")
            if (d.isEmpty() && ip.isEmpty()) continue
            sites.getValue(kind).addAll(d); ips.getValue(kind).addAll(ip)
            last[kind] = i
        }
        if (sites.values.all { it.isEmpty() } && ips.values.all { it.isEmpty() }) return null
        val order = (last.entries.sortedBy { it.value }.map { it.key } + listOf("block", "proxy", "direct")).distinct().joinToString("-")
        return base.copy(
            globalProxy = true,
            directSites = sites.getValue("direct").toList(), directIp = ips.getValue("direct").toList(),
            proxySites = sites.getValue("proxy").toList(), proxyIp = ips.getValue("proxy").toList(),
            blockSites = sites.getValue("block").toList(), blockIp = ips.getValue("block").toList(),
            routeOrder = order.takeIf { it in RouteProfile.ORDERS } ?: RouteProfile.DEFAULT_ORDER,
            domainStrategy = routing.optString("domainStrategy").takeIf { it in RouteProfile.STRATEGIES } ?: base.domainStrategy,
        )
    }
}
