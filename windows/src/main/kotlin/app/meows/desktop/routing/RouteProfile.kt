package app.meows.desktop.routing

import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/** Профиль маршрутизации в виде, удобном редактору. В ядро и в хранилище уходит JSON формата Happ (см. [toJson]). */
data class RouteProfile(
    val id: String = "",
    val name: String = "",
    val globalProxy: Boolean = true,
    val directSites: List<String> = emptyList(), val directIp: List<String> = emptyList(),
    val proxySites: List<String> = emptyList(), val proxyIp: List<String> = emptyList(),
    val blockSites: List<String> = emptyList(), val blockIp: List<String> = emptyList(),
    val routeOrder: String = DEFAULT_ORDER,
    val domainStrategy: String = "IPIfNonMatch",
    val remoteType: String = "DoH", val remoteDomain: String = "", val remoteIp: String = "",
    val localType: String = "DoU", val localDomain: String = "", val localIp: String = "",
    val dnsHosts: Map<String, String> = emptyMap(),   // домен → IP; в редакторе не показывается, но сохраняется (есть в RoscomVPN)
    val geoipUrl: String = "", val geositeUrl: String = "",   // свои гео-файлы профиля; пусто — из подписки, иначе вшитые
) {
    /** Число правил (строк во всех списках) — для подписи в списке профилей. */
    val ruleCount get() = (directSites + directIp + proxySites + proxyIp + blockSites + blockIp).size

    fun toJson(): String = JSONObject()
        .put("Name", name).put("GlobalProxy", globalProxy.toString())
        .put("DirectSites", JSONArray(directSites)).put("DirectIp", JSONArray(directIp))
        .put("ProxySites", JSONArray(proxySites)).put("ProxyIp", JSONArray(proxyIp))
        .put("BlockSites", JSONArray(blockSites)).put("BlockIp", JSONArray(blockIp))
        .put("RouteOrder", routeOrder).put("DomainStrategy", domainStrategy)
        .put("RemoteDNSType", remoteType).put("RemoteDNSDomain", remoteDomain).put("RemoteDNSIP", remoteIp)
        .put("DomesticDNSType", localType).put("DomesticDNSDomain", localDomain).put("DomesticDNSIP", localIp)
        .put("DnsHosts", JSONObject(dnsHosts))
        .put("Geoipurl", geoipUrl).put("Geositeurl", geositeUrl)
        .toString()

    /** Ссылка для обмена в формате Happ: `happ://routing/add/<base64 JSON>` (её понимают Happ, INCY и Meows). */
    fun toLink(): String = "happ://routing/add/" + Base64.getEncoder().encodeToString(toJson().toByteArray(Charsets.UTF_8))

    companion object {
        const val DEFAULT_ORDER = "block-proxy-direct"
        val ORDERS = listOf("block-proxy-direct", "block-direct-proxy", "proxy-block-direct", "proxy-direct-block", "direct-block-proxy", "direct-proxy-block")
        val STRATEGIES = listOf("AsIs", "IPIfNonMatch", "IPOnDemand")
        val DNS_TYPES = listOf("DoH", "DoU", "TCP")

        /** Разбор JSON формата Happ (чужие поля игнорируются, Happ пишет флаги строками). null — не JSON. */
        fun fromJson(id: String, json: String): RouteProfile? = runCatching {
            val o = JSONObject(json)
            fun list(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotEmpty() } } ?: emptyList()
            RouteProfile(
                id, o.optString("Name"), o.optString("GlobalProxy", "true").equals("true", true),
                list("DirectSites"), list("DirectIp"), list("ProxySites"), list("ProxyIp"), list("BlockSites"), list("BlockIp"),
                o.optString("RouteOrder").takeIf { it in ORDERS } ?: DEFAULT_ORDER,
                o.optString("DomainStrategy").takeIf { it in STRATEGIES } ?: "IPIfNonMatch",
                o.optString("RemoteDNSType").takeIf { it in DNS_TYPES } ?: "DoH", o.optString("RemoteDNSDomain"), o.optString("RemoteDNSIP"),
                o.optString("DomesticDNSType").takeIf { it in DNS_TYPES } ?: "DoU", o.optString("DomesticDNSDomain"), o.optString("DomesticDNSIP"),
                o.optJSONObject("DnsHosts")?.let { h -> h.keys().asSequence().associateWith { h.optString(it) } } ?: emptyMap(),
                o.optString("Geoipurl").trim(), o.optString("Geositeurl").trim(),
            )
        }.getOrNull()

        /** Разбор того, что пользователь вставил или отсканировал: ссылка `happ://routing/(on)add/<base64>`, голый base64 или сам JSON. */
        fun fromLink(text: String): RouteProfile? {
            val t = text.trim()
            if (t.startsWith("{")) return fromJson("", t)
            val b64 = t.substringAfterLast('/').trim()
            val json = runCatching { String(Base64.getMimeDecoder().decode(b64), Charsets.UTF_8) }.getOrNull() ?: return null
            return fromJson("", json)
        }
    }
}
