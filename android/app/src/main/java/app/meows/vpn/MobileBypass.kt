package app.meows.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * «Обход в мобильной сети»: при переходе с Wi‑Fi на мобильный интернет Meows подключается к серверу обхода (белые списки),
 * при возврате на Wi‑Fi — к прежнему серверу. Серверы обхода — по названию («обход», «белые списки») или выбранные вручную;
 * они могут лежать в любой из подписок. Выключено по умолчанию. Слежение за сетью — [MobileBypassWatcher].
 */
object MobileBypass {
    const val P_ON = "mobBypass"
    private const val P_SEL = "mobBypassSel"      // выбранные вручную («слот|название»); нет значения — действует поиск по названию
    private const val P_PREV = "mobBypassPrev"    // «слот|название» сервера, на который вернёмся на Wi‑Fi; есть значение — сейчас обход
    const val NONE = 0
    const val WIFI = 1
    const val CELL = 2
    private val nameRule = Regex("обход|бел(?:ый|ые) списк", RegexOption.IGNORE_CASE)

    /** Сервер [server] подписки [store] под номером [index]. */
    class Entry(val store: XKStore, val index: Int, val server: XKStore.Server) {
        val key get() = key(store.id(), server.title)
    }

    /** Что сделано: подключились к серверу обхода ([back] = false) или вернулись на прежний. */
    class Change(val to: String, val back: Boolean)

    /** Сеть, на которой пользователь сам выбрал сервер (0 — не выбирал): пока сеть та же, обход не вмешивается. */
    @Volatile var manual = NONE

    fun key(slot: Int, title: String) = "$slot|$title"

    private fun prefs(ctx: Context) = XKStore.get(ctx).prefs()
    fun isOn(ctx: Context) = prefs(ctx).getBoolean(P_ON, false)
    fun isAuto(s: XKStore.Server) = nameRule.containsMatchIn(s.title)

    /** Все серверы всех подписок: активная подписка первой. */
    fun entries(ctx: Context): List<Entry> {
        val active = XKStore.activeId(ctx)
        return XKStore.all(ctx).filter { it.hasSubscription() }.sortedByDescending { it.id() == active }
            .flatMap { st -> st.servers().mapIndexed { i, s -> Entry(st, i, s) } }
    }

    fun selectedKeys(ctx: Context, all: List<Entry> = entries(ctx)): Set<String> =
        prefs(ctx).getStringSet(P_SEL, null) ?: all.filter { isAuto(it.server) }.map { it.key }.toSet()

    fun chosen(ctx: Context): List<Entry> { val all = entries(ctx); val sel = selectedKeys(ctx, all); return all.filter { it.key in sel } }

    fun toggle(ctx: Context, key: String) {
        val cur = selectedKeys(ctx).toMutableSet()
        if (!cur.add(key)) cur.remove(key)
        prefs(ctx).edit().putStringSet(P_SEL, cur).apply()
    }

    /** Ручной выбор сервера пользователем: возвращаться на «прежний» сервер больше не нужно, а на этой сети обход не вмешивается. */
    fun userPicked(ctx: Context) {
        prefs(ctx).edit().remove(P_PREV).apply()
        manual = transport(ctx)
    }

    private fun current(ctx: Context): Entry? { val st = XKStore.get(ctx); return entries(ctx).firstOrNull { it.store.id() == st.id() && it.index == st.index() } }

    /** Нужно ли сейчас переключаться на обход (мобильная сеть, функция включена, текущий сервер не из обходных). */
    fun pending(ctx: Context): Boolean {
        if (!isOn(ctx) || transport(ctx) != CELL || manual == CELL) return false
        val chosen = chosen(ctx)
        val cur = current(ctx)
        return chosen.isNotEmpty() && chosen.none { it.key == cur?.key }
    }

    /** По какой сети идёт интернет в обход VPN. Мобильная сеть без «проверенного» доступа тоже считается: при белых списках проверка Android не проходит. */
    fun transport(ctx: Context): Int {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var validWifi = false; var validCell = false; var wifi = false; var cell = false
        for (n in cm.allNetworks) {
            val c = cm.getNetworkCapabilities(n) ?: continue
            if (c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) || !c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
            val ok = c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) { wifi = true; validWifi = validWifi || ok }
            else if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) { cell = true; validCell = validCell || ok }
        }
        return when {
            validWifi -> WIFI
            validCell -> CELL
            wifi -> WIFI
            cell -> CELL
            else -> NONE
        }
    }

    /**
     * Приводит выбранный сервер в соответствие с сетью. Блокирует поток (пинг серверов) — вызывать не из главного.
     * @return что изменено (нужен перезапуск туннеля) или null.
     */
    @JvmStatic @Synchronized
    fun evaluate(ctx: Context): Change? {
        if (!isOn(ctx)) return null
        val t = transport(ctx)
        if (manual != NONE) { if (manual == t) return null else manual = NONE }
        return when (t) { CELL -> goBypass(ctx); WIFI -> goBack(ctx); else -> null }
    }

    /** Выключение функции: если сейчас обход — вернуть прежний сервер. */
    @Synchronized
    fun disable(ctx: Context): Change? = goBack(ctx)

    private fun goBypass(ctx: Context): Change? {
        val chosen = chosen(ctx)
        val cur = current(ctx)
        if (chosen.isEmpty() || chosen.any { it.key == cur?.key }) return null
        val target = firstResponding(chosen) ?: return null
        val sp = prefs(ctx)
        if (sp.getString(P_PREV, null) == null && cur != null) sp.edit().putString(P_PREV, cur.key).apply()
        XKStore.activate(ctx, target.store.id()); target.store.setIndex(target.index)
        return Change(target.server.title, back = false)
    }

    private fun goBack(ctx: Context): Change? {
        val sp = prefs(ctx)
        val prev = sp.getString(P_PREV, null) ?: return null
        sp.edit().remove(P_PREV).apply()
        val slot = prev.substringBefore('|').toIntOrNull() ?: return null
        if (slot !in XKStore.slotIds(ctx)) return null
        val st = XKStore.byId(ctx, slot)
        val title = prev.substringAfter('|')
        val idx = st.servers().indexOfFirst { it.title == title }
        if (idx < 0) return null
        val now = XKStore.get(ctx)
        if (now.id() == slot && now.index() == idx) return null
        XKStore.activate(ctx, slot); st.setIndex(idx)
        return Change(title, back = true)
    }

    /** Первый из выбранных серверов, который отвечает (серверы с UDP-протоколом проверить нельзя — берём их, если больше никто не ответил). */
    private fun firstResponding(chosen: List<Entry>): Entry? {
        for ((st, list) in chosen.groupBy { it.store }) pingBlocking(st, list.map { it.index }.toIntArray())
        return chosen.firstOrNull { (it.store.pings[it.index] ?: -1) > 0 } ?: chosen.firstOrNull { it.store.pings[it.index] == -2 }
    }

    private fun pingBlocking(st: XKStore, only: IntArray) {
        val l = CountDownLatch(1)
        st.pingSome(only, Runnable { }, Runnable { l.countDown() })
        l.await(90, TimeUnit.SECONDS)
    }
}
