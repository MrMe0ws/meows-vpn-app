package app.meows.desktop.routing

import app.meows.desktop.geo.GeoUpdater
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeoPinsTest {
    private val ip = "https://cdn.example.com/geoip.dat"
    private val site = "https://cdn.example.com/geosite.dat"

    @Test fun pinsNeedBothHttpsLinks() {
        assertEquals(GeoUpdater.Pins(ip, site), GeoUpdater.pinsOf(" $ip ", site))
        assertNull(GeoUpdater.pinsOf(ip, ""))
        assertNull(GeoUpdater.pinsOf("http://x/geoip.dat", site))
    }

    @Test fun headerPins() {
        val json = """{"Name":"X","Geoipurl":"$ip","Geositeurl":"$site"}"""
        val header = "happ://routing/add/" + Base64.getEncoder().encodeToString(json.toByteArray())
        assertEquals(GeoUpdater.Pins(ip, site), GeoUpdater.parsePins(header))
        assertNull(GeoUpdater.parsePins(null))
        assertNull(GeoUpdater.parsePins("happ://routing/add/@@@"))
    }

    @Test fun profileKeepsGeoLinksInLink() {
        val p = RouteProfile(name = "G", geoipUrl = ip, geositeUrl = site)
        val back = RouteProfile.fromLink(p.toLink())
        assertEquals(ip, back?.geoipUrl); assertEquals(site, back?.geositeUrl)
    }

    @Test fun roscomBuiltinHasPins() {
        val r = RoutingProfiles.builtinDefault(RoutingProfiles.ROSCOM)
        assertTrue(GeoUpdater.pinsOf(r.geoipUrl, r.geositeUrl) != null)
        assertTrue(RoutingProfiles.builtinDefault(RoutingProfiles.MEOWS).geoipUrl.isEmpty())
    }
}
