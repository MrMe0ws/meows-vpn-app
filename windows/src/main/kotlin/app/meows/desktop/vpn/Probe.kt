package app.meows.desktop.vpn

import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

/** Проверка «интернет через VPN работает»: запрос HTTP 204 за 6 секунд. */
object Probe {
    private const val PROBE_URL = "http://cp.cloudflare.com/generate_204"

    /** [proxyPort] — порт локального HTTP-прокси (режим «Системный прокси»); null — идём обычным путём (в TUN он ведёт через VPN). */
    fun viaVpn(proxyPort: Int?): Boolean = runCatching {
        val u = URL(PROBE_URL)
        val c = (if (proxyPort != null) u.openConnection(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", proxyPort))) else u.openConnection(Proxy.NO_PROXY)) as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 6000; c.instanceFollowRedirects = false
        val code = c.responseCode
        c.disconnect()
        code in 200..399
    }.getOrDefault(false)
}
