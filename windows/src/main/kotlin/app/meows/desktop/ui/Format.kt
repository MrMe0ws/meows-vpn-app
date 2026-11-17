package app.meows.desktop.ui

import java.util.Locale

fun formatBytesRu(b: Long): String {
    if (b <= 0) return "0 Б"
    val u = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ")
    var v = b.toDouble(); var i = 0
    while (v >= 1024 && i < u.lastIndex) { v /= 1024; i++ }
    return String.format(Locale.US, if (v >= 100 || i == 0) "%.0f %s" else "%.1f %s", v, u[i]).replace('.', ',')
}

fun formatTimer(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
}