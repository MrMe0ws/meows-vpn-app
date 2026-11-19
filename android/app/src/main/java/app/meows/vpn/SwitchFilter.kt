package app.meows.vpn

import app.meows.vpn.i18n.serversWord
import app.meows.vpn.i18n.trf
import app.meows.vpn.i18n.tr
import android.content.SharedPreferences

/**
 * На какие серверы можно автопереключаться. Режим «Все, кроме» (0) или «Выбранные» (1, только отмеченные); у каждого режима свой список (по названию сервера).
 * Пока пользователь не менял список исключений, исключены российские серверы (флаг 🇷🇺): через них не открываются заблокированные сайты.
 */
object SwitchFilter {
    const val P_MODE = "switchMode"
    private const val P_EXCL = "switchExcl"     // отсутствует — действует правило по умолчанию
    private const val P_ONLY = "switchOnly"
    private const val RU_FLAG = "🇷🇺"

    fun mode(sp: SharedPreferences) = sp.getInt(P_MODE, 0)
    fun setMode(sp: SharedPreferences, m: Int) = sp.edit().putInt(P_MODE, m).apply()

    /** Отмеченные в текущем режиме серверы (исключённые или разрешённые). */
    fun marked(sp: SharedPreferences, servers: List<XKStore.Server>): Set<String> =
        if (mode(sp) == 1) sp.getStringSet(P_ONLY, emptySet()).orEmpty()
        else sp.getStringSet(P_EXCL, null) ?: servers.filter { it.flag == RU_FLAG }.map { it.title }.toSet()

    fun toggle(sp: SharedPreferences, servers: List<XKStore.Server>, title: String) {
        val cur = marked(sp, servers).toMutableSet()
        if (!cur.add(title)) cur.remove(title)
        sp.edit().putStringSet(if (mode(sp) == 1) P_ONLY else P_EXCL, cur).apply()
    }

    @JvmStatic
    fun allowed(sp: SharedPreferences, servers: List<XKStore.Server>, s: XKStore.Server): Boolean =
        (s.title in marked(sp, servers)) == (mode(sp) == 1)

    /** Краткий итог для строки настроек: названия серверов длинные, поэтому только количество. */
    fun summary(sp: SharedPreferences, servers: List<XKStore.Server>): String {
        val m = marked(sp, servers)
        val n = servers.map { it.title }.distinct().count { it in m }
        val word = serversWord(n)
        return if (mode(sp) == 1) (if (n == 0) tr("Ничего не выбрано") else trf("Только %d %s", n, word))
        else (if (n == 0) tr("Все серверы") else trf("Все, кроме %d %s", n, word))
    }
}
