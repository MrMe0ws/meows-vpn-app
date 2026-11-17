package app.meows.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkNormalizerTest {
    private val want = "https://sub.me0ws.store/AbC123xyz"
    private fun n(s: String?) = LinkNormalizer.normalize(s)

    @Test fun plain() { assertEquals(want, n("meows://add/https://sub.me0ws.store/AbC123xyz")) }
    @Test fun alreadyHttps() { assertEquals(want, n(want)) }
    @Test fun happPrefix() { assertEquals(want, n("happ://add/https://sub.me0ws.store/AbC123xyz")) }
    @Test fun upperCaseScheme() { assertEquals(want, n("MEOWS://ADD/HTTPS://sub.me0ws.store/AbC123xyz")) }
    @Test fun mixedCase() { assertEquals(want, n("Meows://Add/Https://sub.me0ws.store/AbC123xyz")) }
    @Test fun urlEncoded() { assertEquals(want, n("meows://add/https%3A%2F%2Fsub.me0ws.store%2FAbC123xyz")) }
    @Test fun urlEncodedLower() { assertEquals(want, n("meows://add/https%3a%2f%2fsub.me0ws.store%2fAbC123xyz")) }
    @Test fun colonThenEncodedSlashes() { assertEquals(want, n("meows://add/https:%2F%2Fsub.me0ws.store%2FAbC123xyz")) }
    @Test fun noColon() { assertEquals(want, n("meows://add/https//sub.me0ws.store/AbC123xyz")) }
    @Test fun singleSlash() { assertEquals(want, n("meows://add/https:/sub.me0ws.store/AbC123xyz")) }
    @Test fun tripleSlash() { assertEquals(want, n("meows://add/https:///sub.me0ws.store/AbC123xyz")) }
    @Test fun quotes() { assertEquals(want, n("\"meows://add/https://sub.me0ws.store/AbC123xyz\"")) }
    @Test fun spacesAndQuotes() { assertEquals(want, n("  'meows://add/https://sub.me0ws.store/AbC123xyz'  \n")) }
    @Test fun browserTrailingSlash() { assertEquals(want, n("meows://add/https://sub.me0ws.store/AbC123xyz/")) }
    @Test fun hostOnlyKeepsSlash() { assertEquals("https://sub.me0ws.store/", n("meows://add/https://sub.me0ws.store/")) }
    @Test fun queryKept() { assertEquals("$want?a=1&b=2", n("meows://add/$want?a=1&b=2")) }
    @Test fun plusNotSpace() { assertEquals("https://h.example/a+b", n("meows://add/https%3A%2F%2Fh.example%2Fa+b")) }
    @Test fun httpAllowed() { assertTrue(LinkNormalizer.isHttpLink(n("meows://add/http://10.0.0.1:3000/tok"))) }
    @Test fun garbage() { assertFalse(LinkNormalizer.isHttpLink(n("hello world"))) }
    @Test fun empty() { assertFalse(LinkNormalizer.isHttpLink(n(null))) }
    @Test fun headHasNoToken() { assertEquals("meows://add/https://sub.me…", LinkNormalizer.head("meows://add/https://sub.me0ws.store/AbC123xyz")) }
}
