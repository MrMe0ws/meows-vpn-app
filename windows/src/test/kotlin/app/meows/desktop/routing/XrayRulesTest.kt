package app.meows.desktop.routing

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XrayRulesTest {
    /** Структура правил реального шаблона магазина: DNS-порт, блок, DNS-IP, «через VPN» (с copilot.microsoft.com), «напрямую» (с geosite:microsoft). */
    private val server = """{"remarks":"A","outbounds":[{"tag":"proxy","protocol":"vless"},{"tag":"direct","protocol":"freedom"},{"tag":"block","protocol":"blackhole"},{"tag":"dns-out","protocol":"dns"}],
      "routing":{"domainStrategy":"IPIfNonMatch","rules":[
        {"type":"field","port":"53","outboundTag":"dns-out"},
        {"type":"field","domain":["geosite:win-spy","geosite:category-ads"],"outboundTag":"block"},
        {"type":"field","ip":["77.88.8.8"],"outboundTag":"direct"},
        {"type":"field","ip":["1.1.1.1","8.8.8.8"],"outboundTag":"proxy"},
        {"type":"field","domain":["geosite:youtube","domain:copilot.microsoft.com"],"outboundTag":"proxy"},
        {"type":"field","domain":["geosite:category-ru","geosite:microsoft"],"outboundTag":"direct"},
        {"type":"field","ip":["geoip:private","geoip:direct"],"outboundTag":"direct"},
        {"type":"field","protocol":["bittorrent"],"outboundTag":"direct"}]}}"""

    private fun derive(body: String, index: Int = 0): RouteProfile? =
        XrayRules.config(body, index)?.let { XrayRules.profileFrom(it, RouteProfile(name = "MeowsVPN JSON", remoteType = "DoU", remoteIp = "1.1.1.1")) }

    @Test fun rulesAreGroupedByTarget() {
        val p = assertNotNull(derive("[$server]"))
        assertEquals(listOf("geosite:win-spy", "geosite:category-ads"), p.blockSites)
        assertEquals(listOf("geosite:youtube", "domain:copilot.microsoft.com"), p.proxySites)
        assertEquals(listOf("1.1.1.1", "8.8.8.8"), p.proxyIp)
        assertEquals(listOf("geosite:category-ru", "geosite:microsoft"), p.directSites)
        assertEquals(listOf("77.88.8.8", "geoip:private", "geoip:direct"), p.directIp)
    }

    @Test fun orderKeepsProxyBeforeDirectSoOverlapsStayProxied() {
        // copilot.microsoft.com перекрывается с geosite:microsoft: в шаблоне «через VPN» выше «напрямую», в профиле должно быть так же
        assertEquals("block-proxy-direct", assertNotNull(derive(server)).routeOrder)
    }

    @Test fun headerNameDnsAndStrategyAreKept() {
        val p = assertNotNull(derive(server))
        assertEquals("MeowsVPN JSON", p.name); assertEquals("DoU", p.remoteType); assertEquals("IPIfNonMatch", p.domainStrategy)
        assertTrue(p.globalProxy)
    }

    @Test fun picksServerByIndexAndAcceptsBase64() {
        val lte = """{"outbounds":[{"tag":"direct","protocol":"freedom"}],"routing":{"rules":[{"type":"field","domain":["domain:nalog.ru"],"outboundTag":"direct"}]}}"""
        val body = Base64.getEncoder().encodeToString("[$server,$lte]".toByteArray())
        assertEquals(listOf("domain:nalog.ru"), assertNotNull(derive(body, 1)).directSites)
        assertEquals(listOf("geosite:youtube", "domain:copilot.microsoft.com"), assertNotNull(derive(body, 0)).proxySites)
    }

    @Test fun noDomainOrIpRulesMeansNull() {
        assertNull(derive("""{"outbounds":[],"routing":{"rules":[{"type":"field","protocol":["bittorrent"],"outboundTag":"direct"}]}}"""))
        assertNull(derive("не json"))
    }
}
