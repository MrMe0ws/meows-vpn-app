package app.meows.desktop.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteProfileTest {
    private val p = RouteProfile(
        name = "Тест", globalProxy = false, directSites = listOf("geosite:category-ru", "domain:example.com"), proxyIp = listOf("8.8.8.8"),
        routeOrder = "proxy-block-direct", domainStrategy = "AsIs", remoteType = "TCP", remoteIp = "1.1.1.1",
        dnsHosts = mapOf("lknpd.nalog.ru" to "213.24.64.181"),
    )

    @Test fun linkRoundTrip() {
        val link = p.toLink()
        assertTrue(link.startsWith("happ://routing/add/"))
        assertEquals(p.copy(id = ""), RouteProfile.fromLink(link))
    }

    @Test fun acceptsRawJsonAndBareBase64() {
        assertEquals(p.copy(id = ""), RouteProfile.fromLink(p.toJson()))
        assertEquals(p.copy(id = ""), RouteProfile.fromLink(p.toLink().substringAfterLast('/')))
    }

    @Test fun happStyleFlagsAsStrings() {
        val q = RouteProfile.fromJson("", """{"Name":"X","GlobalProxy":"false","DirectSites":["a"],"RouteOrder":"bogus"}""")
        assertNotNull(q)
        assertEquals(false, q.globalProxy)
        assertEquals(RouteProfile.DEFAULT_ORDER, q.routeOrder)
    }

    @Test fun garbageIsRejected() {
        assertNull(RouteProfile.fromLink("не профиль"))
        assertNull(RouteProfile.fromLink("happ://routing/add/@@@"))
    }

    @Test fun builtinsMatchUpstreamShape() {
        val r = RoutingProfiles.builtinDefault(RoutingProfiles.ROSCOM)
        assertEquals("RoscomVPN", r.name)
        assertTrue("geosite:youtube" in r.proxySites && "geosite:category-ads" in r.blockSites)
    }
}
