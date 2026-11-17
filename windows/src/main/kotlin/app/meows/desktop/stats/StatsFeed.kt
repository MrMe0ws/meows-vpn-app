package app.meows.desktop.stats

/**
 * Счётчики трафика от ядра: строка `XKSTAT <VPN отправлено> <VPN получено> <напрямую отправлено> <напрямую получено>` раз в секунду
 * (накопленные байты). Скорость — разница соседних строк, график хранит последние 2 минуты. Замер скорости серверов не делаем.
 * Та же логика в Android (`app/meows/vpn/stats/StatsFeed.kt`) — менять в обоих местах.
 */
object StatsFeed {
    /** Скорость за последнюю секунду, байт/с (VPN + напрямую вместе). */
    data class Sample(val down: Long, val up: Long)

    /** Накопленные байты за подключение. */
    data class Totals(val vpnUp: Long = 0, val vpnDown: Long = 0, val directUp: Long = 0, val directDown: Long = 0) {
        val down get() = vpnDown + directDown
        val up get() = vpnUp + directUp
    }

    private const val KEEP = 120
    private val buf = ArrayDeque<Sample>()
    private var last: Totals? = null
    @Volatile private var current = Totals()

    /** true — строка была счётчиком и разобрана (в логи её не пишем). */
    @JvmStatic @Synchronized fun accept(line: String): Boolean {
        if (!line.startsWith("XKSTAT ")) return false
        val p = line.trim().split(' ')
        if (p.size < 5) return true
        val n = p.drop(1).map { it.toLongOrNull() ?: return true }
        val cur = Totals(n[0], n[1], n[2], n[3])
        val l = last
        val sample = if (l == null || cur.down < l.down || cur.up < l.up) Sample(0, 0) else Sample(cur.down - l.down, cur.up - l.up)
        buf.addLast(sample); while (buf.size > KEEP) buf.removeFirst()
        last = cur; current = cur
        return true
    }

    @JvmStatic @Synchronized fun reset() { buf.clear(); last = null; current = Totals() }

    @JvmStatic @Synchronized fun samples(): List<Sample> = buf.toList()

    @JvmStatic fun totals(): Totals = current
}
