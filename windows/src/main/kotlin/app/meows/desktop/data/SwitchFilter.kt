package app.meows.desktop.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/**
 * На какие серверы можно автопереключаться. Режим «Все, кроме» (0) или «Выбранные» (1, только отмеченные); у каждого режима свой список (по названию сервера).
 * Пока пользователь не менял список исключений, исключены российские серверы (флаг 🇷🇺): через них не открываются заблокированные сайты.
 */
class SwitchFilter(private val store: SettingsStore) {
    var mode by mutableIntStateOf(store.int("switchMode", 0)); private set
    private var excl by mutableStateOf(load("switchExcl"))   // null — действует правило по умолчанию
    private var only by mutableStateOf(load("switchOnly") ?: emptySet())

    private fun load(k: String): Set<String>? = store.str(k).takeIf { it.isNotEmpty() }
        ?.let { s -> runCatching { JSONArray(s).let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } }.getOrNull() }

    fun chooseMode(m: Int) { mode = m; store.set("switchMode", m) }

    /** Отмеченные в текущем режиме серверы (исключённые или разрешённые). */
    fun marked(servers: List<Server>): Set<String> =
        if (mode == 1) only else excl ?: servers.filter { it.flag == RU_FLAG }.map { it.title }.toSet()

    fun toggle(servers: List<Server>, title: String) {
        val cur = marked(servers).toMutableSet()
        if (!cur.add(title)) cur.remove(title)
        store.set(if (mode == 1) "switchOnly" else "switchExcl", JSONArray(cur.toList()).toString())
        if (mode == 1) only = cur else excl = cur
    }

    fun allowed(servers: List<Server>, s: Server) = (s.title in marked(servers)) == (mode == 1)

    /** Краткий итог для строки настроек: названия серверов длинные, поэтому только количество. */
    fun summary(servers: List<Server>): String {
        val m = marked(servers)
        val n = servers.map { it.title }.distinct().count { it in m }
        val word = if (n % 10 == 1 && n % 100 != 11) "сервер" else if (n % 10 in 2..4 && n % 100 !in 12..14) "сервера" else "серверов"
        return if (mode == 1) (if (n == 0) "Ничего не выбрано" else "Только $n $word")
        else (if (n == 0) "Все серверы" else "Все, кроме $n $word")
    }

    private companion object { const val RU_FLAG = "🇷🇺" }
}
