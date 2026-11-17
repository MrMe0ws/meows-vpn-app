package app.meows.desktop.log

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date

data class LogLine(val time: Long, val tag: String, val text: String)

enum class Route { Direct, Vpn, Block }

/** Одно соединение: куда (хост:порт, протокол) и каким путём пошло по правилам маршрутизации. */
data class TrafficLine(val time: Long, val proto: String, val dest: String, val route: Route)

/**
 * Журнал в памяти: события приложения (~1500 строк) и отдельно — соединения (~2000), чтобы поток соединений не вытеснял события.
 * Ссылки подписки сюда не пишем.
 */
object AppLog {
    val lines = mutableStateListOf<LogLine>()
    val traffic = mutableStateListOf<TrafficLine>()
    private val fmt = SimpleDateFormat("HH:mm:ss")
    // «from 127.0.0.1:5 accepted tcp:host:443 [tun-in >> proxy]» (TUN) или «accepted //host:443 [http-in -> direct]» (прокси-режим)
    private val accepted = Regex("""accepted (?:(tcp|udp):)?(?://)?(\S+)(?: \[(.+?)])?""")
    // «taking detour [выход] for [tcp:www.google.com:443]» — здесь домен из SNI даже в TUN (в «accepted» остаётся IP); идёт строкой перед «accepted»
    private val detour = Regex("""taking detour \[(.*?)] for \[(.*?)]""")
    private val ipv4 = Regex("""\d+\.\d+\.\d+\.\d+""")
    private var pending: Pair<String, String>? = null      // (tcp:домен:порт, тег выхода)
    private val answer = Regex("""got answer: (\S+) \S+ -> \[(.*?)]""")   // «got answer: www.google.com. TypeA -> [1.2.3.4 …], rtt: …»
    private val names = object : LinkedHashMap<String, String>() {   // IP → домен из DNS-ответов, ~5000 последних
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, String>?) = size > 5000
    }

    @Synchronized fun add(tag: String, text: String) {
        lines.add(LogLine(System.currentTimeMillis(), tag, text))
        if (lines.size > 1500) lines.subList(0, 300).clear()
    }

    /** Строка из вывода ядра: журнал доступа («accepted …») → соединения (возвращает true — строка поглощена), остальное → события ядра. */
    fun addCoreLine(line: String): Boolean {
        detour.find(line)?.let { d -> synchronized(this) { pending = d.groupValues[2] to d.groupValues[1].lowercase() }; return true }
        answer.find(line)?.let { a -> synchronized(this) { a.groupValues[2].split(" ").filter { it.isNotBlank() }.forEach { ip -> names[ip] = a.groupValues[1].trimEnd('.') } }; return true }
        if ("[Info]" in line) return true                  // подробности ядра не показываем
        if ("from DNS accepted" in line) { synchronized(this) { pending = null }; return true }       // собственные DNS-запросы ядра
        val m = accepted.find(line)
        if (m != null) { addTraffic(m.groupValues[1].ifEmpty { "tcp" }, m.groupValues[2], m.groupValues[3]); return true }
        add("CORE", line)
        return false
    }

    @Synchronized private fun addTraffic(proto: String, destRaw: String, detour: String) {
        // «https://host/путь» (запросы DoH и т. п.) → «host:443»; обычное «host:порт» не трогаем
        val dest = if (destRaw.startsWith("http://") || destRaw.startsWith("https://")) destRaw.substringAfter("://").substringBefore('/').let { if (':' in it) it else it + (if (destRaw.startsWith("https")) ":443" else ":80") } else destRaw
        val out = detour.substringAfterLast(">>").substringAfterLast("->").trim().lowercase()
        val route = when {
            "dns" in out -> return                          // внутренние DNS-запросы не показываем
            "direct" in out || "freedom" in out -> Route.Direct
            "block" in out || "blackhole" in out || "reject" in out -> Route.Block
            else -> Route.Vpn
        }
        if (dest.startsWith("127.") || dest.startsWith("[::1]")) return
        val port = dest.substringAfterLast(":")
        val host = dest.substringBeforeLast(":").removePrefix("[").removeSuffix("]")
        val fromSni = pending?.takeIf { it.second == out }?.first?.substringAfter(":")?.takeIf { r ->
            val h = r.substringBeforeLast(":"); r.substringAfterLast(":") == port && !h.startsWith("[") && !ipv4.matches(h)
        }
        pending = null
        val shown = fromSni ?: names[host]?.let { "$it:$port" } ?: dest
        val last = traffic.lastOrNull()
        val now = System.currentTimeMillis()
        if (last != null && last.dest == shown && last.route == route && now - last.time < 3000) return   // повторы подряд
        traffic.add(TrafficLine(now, proto.uppercase(), shown, route))
        if (traffic.size > 2000) traffic.subList(0, 400).clear()
    }

    @Synchronized fun clear() { lines.clear(); traffic.clear() }

    fun format(l: LogLine) = "[${fmt.format(Date(l.time))}] [${l.tag}] ${l.text}"
    fun time(t: Long): String = fmt.format(Date(t))
    fun format(t: TrafficLine) = "[${time(t.time)}] ${t.proto} ${t.dest} → ${when (t.route) { Route.Direct -> "напрямую"; Route.Vpn -> "через VPN"; Route.Block -> "заблокировано" }}"
}
