package app.meows.desktop.data

/**
 * Приводит ссылку подписки к виду `https://…` из любого вида, в котором её может передать браузер или ОС:
 * `meows://add/https://…`, `MEOWS://ADD/…`, в кавычках/с пробелами, `https%3A%2F%2F…`, `https//…`, `https:/…`, лишний `/` в конце.
 * Логика та же, что в Android-версии (`app/meows/vpn/LinkNormalizer.kt`) — менять в обоих местах.
 */
object LinkNormalizer {
    private val appScheme = Regex("^(?:meows|happ):(?://)?add(?:/|$)", RegexOption.IGNORE_CASE)
    private val encodedHttp = Regex("^https?(?:%3A|:)(?:%2F|%3A)", RegexOption.IGNORE_CASE)
    private val httpScheme = Regex("^(https?)(?::/*|//+)", RegexOption.IGNORE_CASE)
    private const val QUOTES = "\"'`«»“”‘’<>"

    fun normalize(raw: String?): String {
        var s = clean(raw.orEmpty())
        s = clean(appScheme.replace(s, ""))
        if (encodedHttp.containsMatchIn(s)) s = percentDecode(s)
        s = httpScheme.replace(s) { it.groupValues[1].lowercase() + "://" }
        return dropBrowserSlash(clean(s))
    }

    /** Похоже ли на ссылку подписки после нормализации. */
    fun isHttpLink(s: String) = s.startsWith("https://") || s.startsWith("http://")

    /** Короткое безопасное описание для лога: только начало (схема и хост), без токена. */
    fun head(raw: String?): String = raw.orEmpty().take(26).let { if ((raw?.length ?: 0) > 26) "$it…" else it }

    private fun clean(s: String) = s.trim { it.isWhitespace() || it in QUOTES }

    /** Браузер дописывает «/» к адресу без пути; у подписки токен — часть пути, лишний «/» в конце убираем. */
    private fun dropBrowserSlash(s: String): String {
        if (!s.endsWith("/") || !isHttpLink(s)) return s
        val path = s.substringAfter("://").substringAfter('/', "")
        return if (path.removeSuffix("/").isNotEmpty()) s.removeSuffix("/") else s
    }

    /** Декодирование %XX без превращения «+» в пробел. */
    private fun percentDecode(s: String): String {
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        val b = s.toByteArray(Charsets.UTF_8)
        while (i < b.size) {
            val c = b[i].toInt().toChar()
            if (c == '%' && i + 2 < b.size && hex(b[i + 1]) >= 0 && hex(b[i + 2]) >= 0) { out.write(hex(b[i + 1]) * 16 + hex(b[i + 2])); i += 3 }
            else { out.write(b[i].toInt()); i++ }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private fun hex(x: Byte): Int {
        val c = x.toInt().toChar()
        return when (c) { in '0'..'9' -> c - '0'; in 'a'..'f' -> c - 'a' + 10; in 'A'..'F' -> c - 'A' + 10; else -> -1 }
    }
}
