package app.meows.vpn.ui.components

import app.meows.vpn.i18n.tr
import java.util.Locale

fun formatBytes(b: Long): String {
    if (b <= 0) return tr("0 Б")
    val u = arrayOf(tr("Б"), tr("КБ"), tr("МБ"), tr("ГБ"), tr("ТБ"))
    var v = b.toDouble(); var i = 0
    while (v >= 1024 && i < u.lastIndex) { v /= 1024; i++ }
    return String.format(Locale.US, if (v >= 100 || i == 0) "%.0f %s" else "%.1f %s", v, u[i])
}

/** Размер с десятичной запятой: «184,7 МБ». */
fun formatBytesRu(b: Long): String = formatBytes(b).replace('.', ',')

fun formatTimer(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
}

fun pluralRu(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10; val m100 = n % 100
    return if (m10 == 1 && m100 != 11) one else if (m10 in 2..4 && (m100 < 10 || m100 >= 20)) few else many
}
