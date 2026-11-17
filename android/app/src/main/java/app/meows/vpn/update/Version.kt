package app.meows.vpn.update

/** Сравнение версий вида 2.1.0 / v2.1.0 / 2.1.0-debug. Суффикс после «-» игнорируется. */
object Version {
    fun parse(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V").substringBefore('-').split('.')
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    fun compare(a: String, b: String): Int {
        val x = parse(a); val y = parse(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    fun isNewer(candidate: String, current: String) = compare(candidate, current) > 0
}
