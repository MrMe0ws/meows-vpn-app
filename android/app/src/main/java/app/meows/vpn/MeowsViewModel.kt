package app.meows.vpn

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import app.meows.vpn.update.UpdateController

/** Связывает старую Java-логику (XKStore / XKVpnService) с Compose-состоянием. */
class MeowsViewModel(app: Application) : AndroidViewModel(app), XKVpnService.Listener {
    /** Активная подписка — та, с которой подключается VPN (остальные видны на главной своими карточками). */
    val st: XKStore get() = XKStore.get(getApplication())
    private val handler = Handler(Looper.getMainLooper())

    var vpnState by mutableIntStateOf(XKVpnService.state); private set
    /** Растёт при любом изменении подписки, серверов, пингов или настроек. */
    var rev by mutableIntStateOf(0); private set
    /** Какие подписки сейчас обновляются / пингуются (по номеру слота) и какие серверы пингуются по одному (слот*1000 + индекс). */
    private val refreshingSlots = mutableStateSetOf<Int>()
    private val pingingSlots = mutableStateSetOf<Int>()
    private val pingingOne = mutableStateSetOf<Int>()
    val refreshing: Boolean get() = refreshingSlots.isNotEmpty()
    val pinging: Boolean get() = pingingSlots.isNotEmpty()
    fun isRefreshing(s: XKStore) = s.id() in refreshingSlots
    fun isPinging(s: XKStore) = s.id() in pingingSlots
    fun isPingingOne(s: XKStore, i: Int) = (s.id() * 1000 + i) in pingingOne
    fun isActive(s: XKStore) = s.id() == st.id()
    private var pingHidden = false

    val updates = UpdateController(app, st.prefs(), viewModelScope)

    /** Последняя показанная ошибка — попадает в отчёт для поддержки. */
    var lastError: String? = null; private set
    /** Необязательная кнопка в диалоге ошибки (например «Открыть кабинет»). */
    var dialogActionLabel by mutableStateOf<String?>(null); private set
    private var dialogAction: (() -> Unit)? = null

    var dialogTitle by mutableStateOf<String?>(null)
    var dialogText by mutableStateOf<String?>(null)
    var toast by mutableStateOf<String?>(null)

    var themeMode by mutableIntStateOf(st.pref(XKStore.P_THEME, 0))
    var palette by mutableIntStateOf(st.pref("palette", 0))
    /** По умолчанию — цвета из обоев (канон Material You, Android 12+); пока пользователь не выбрал палитру сам. */
    var dynamicColors by mutableStateOf(st.flag("dynamic", android.os.Build.VERSION.SDK_INT >= 31))
    var amoled by mutableStateOf(st.flag("amoled", false))

    private var autoPingDone = false

    init {
        XKAppHolder.ctx = app.applicationContext
        XKVpnService.addListener(this)
        st.scheduleReminders()
        updates.checkOnLaunch()
        if (st.url().isNotEmpty() && (st.updateOnLaunch() || st.needsAutoUpdate())) refresh(true)
        else autoPing()
    }

    override fun onCleared() {
        XKVpnService.removeListener(this)
        super.onCleared()
    }

    override fun onState(state: Int) {
        vpnState = state
        bump()   // сервис мог сам сменить сервер (автопереключение) — обновляем список и выбранный сервер
        if (state == XKVpnService.IDLE) {
            XKVpnService.lastError?.let { alert("Не подключилось", it); XKVpnService.lastError = null }
        }
    }

    fun alert(title: String, text: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        dialogTitle = title; dialogText = text; lastError = text
        dialogActionLabel = actionLabel; dialogAction = action
    }
    fun runDialogAction() { dialogAction?.invoke(); dismissAlert() }
    fun dismissAlert() { dialogTitle = null; dialogText = null; dialogActionLabel = null; dialogAction = null }

    /** Открыть ссылку во внешнем приложении (браузер, Telegram). */
    fun openLink(url: String) {
        runCatching {
            getApplication<Application>().startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** Ошибка загрузки подписки; если похоже на лимит устройств — сразу предлагаем кабинет. */
    private fun fetchError(title: String, error: String) {
        if (error.contains("лимит", ignoreCase = true)) {
            alert(title, error, "Открыть кабинет") { openLink(app.meows.vpn.shop.ShopLinks.cabinet(st)) }
        } else alert(title, error)
    }
    private fun bump() { rev++ }

    // ---------- подписки ----------
    /** Все подписки (карточки на главной). */
    val subs: List<XKStore> get() { rev; return XKStore.all(getApplication()).filter { it.hasSubscription() } }
    val servers: List<XKStore.Server> get() { rev; return st.servers() }
    val selected: Int get() { rev; return st.index() }
    val hasSub: Boolean get() { rev; return XKStore.all(getApplication()).any { it.hasSubscription() } }

    /** Добавить подписку: такая же ссылка уже есть — обновим её, иначе новая карточка. Первая подписка становится активной. */
    fun addSubscription(raw: String) {
        val link = LinkNormalizer.normalize(raw)
        if (!LinkNormalizer.isHttpLink(link)) { fetchError("Не получилось", "Это не похоже на ссылку подписки."); return }
        val hadActive = st.hasSubscription()
        val s = XKStore.slotFor(getApplication(), link)
        fetchInto(s, raw, replaced = false) { if (!hadActive) XKStore.activate(getApplication(), s.id()) }
    }

    /** «Заменить ссылку» в карточке: новая ссылка записывается в эту же подписку. */
    fun replaceSubscription(s: XKStore, raw: String) = fetchInto(s, raw, replaced = true) {}

    private fun fetchInto(s: XKStore, raw: String, replaced: Boolean, onOk: () -> Unit) {
        refreshingSlots.add(s.id())
        s.fetch(raw) { error ->
            refreshingSlots.remove(s.id())
            if (error != null) {
                if (s.url().isEmpty()) XKStore.removeSlot(getApplication(), s.id())   // новая карточка не получилась — не оставляем пустую
                bump(); fetchError("Не получилось", error); return@fetch
            }
            onOk()
            toast = if (replaced) "Ссылка заменена" else "Подписка добавлена"
            if (isActive(s)) { syncGeo(); bump(); autoPing(); reconnectIfNeeded() } else bump()
        }
    }

    fun refresh(silent: Boolean) = refresh(st, silent)

    fun refresh(s: XKStore, silent: Boolean) {
        if (s.url().isEmpty() || s.id() in refreshingSlots) return
        refreshingSlots.add(s.id())
        s.fetch(s.url()) { error ->
            refreshingSlots.remove(s.id())
            if (error != null) { if (!silent) fetchError("Не удалось обновить", error); return@fetch }
            if (!silent) toast = "Подписка обновлена"
            if (isActive(s)) { syncGeo(); bump(); autoPing() } else bump()
        }
    }

    /** Обновить те подписки, которым пора (по их собственному интервалу). */
    fun refreshStale() { subs.filter { it.needsAutoUpdate() }.forEach { refresh(it, true) } }

    /** Гео-файлы следуют ссылкам из заголовка `routing` панели (см. GeoUpdater). */
    private fun syncGeo() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { app.meows.vpn.geo.GeoUpdater.sync(getApplication(), st) } }
    }

    fun deleteSubscription() = deleteSubscription(st)

    fun deleteSubscription(s: XKStore) {
        val wasActive = isActive(s)
        XKStore.removeSlot(getApplication(), s.id())
        bump()
        if (wasActive && vpnState != XKVpnService.IDLE) XKVpnService.stop(getApplication())
    }

    fun copy(text: String, label: String = "meows") {
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun clipboardText(): String {
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(getApplication())?.toString()?.trim().orEmpty()
    }

    /** «Вставить из буфера обмена»: добавляет подписку, если в буфере ссылка. */
    fun addFromClipboard() {
        val t = clipboardText()
        if (t.startsWith("http", true) || t.contains("://add/")) addSubscription(t) else toast = "В буфере нет ссылки подписки"
    }

    // ---------- сервер / пинг ----------
    fun select(i: Int) = select(st, i)

    /** Выбор сервера в любой карточке: подписка карточки становится активной. */
    fun select(s: XKStore, i: Int) {
        if (isActive(s) && i == s.index()) return
        XKStore.activate(getApplication(), s.id()); s.setIndex(i); bump(); reconnectIfNeeded()
    }

    /** «Выбрать быстрейший»: пингуем все серверы подписки и выбираем лучший по результату. */
    fun selectFastest(s: XKStore = st) {
        if (!s.hasSubscription() || isPinging(s)) return
        toast = "Измеряем пинг…"
        pingingSlots.add(s.id()); pingHidden = false
        handler.removeCallbacksAndMessages(PING_TOKEN)
        s.pingAll({ bump() }, { pingingSlots.remove(s.id()); bump(); scheduleHidePing(); pickFastest(s) })
    }

    private fun pickFastest(s: XKStore) {
        val list = s.servers()
        var best = -1; var bestMs = Int.MAX_VALUE
        for (i in list.indices) {
            val ms = s.pings[i] ?: continue
            if (ms in 1 until bestMs) { bestMs = ms; best = i }
        }
        if (best < 0) { toast = "Ни один сервер не ответил"; return }
        toast = "Быстрейший: ${list[best].title} · $bestMs мс"
        select(s, best)
    }

    private fun autoPing() {
        if (autoPingDone || !st.flag(XKStore.P_PING_LAUNCH, false) || !st.hasSubscription()) return
        autoPingDone = true; ping()
    }

    fun ping(s: XKStore = st) {
        if (isPinging(s) || !s.hasSubscription()) return
        pingingSlots.add(s.id()); pingHidden = false
        handler.removeCallbacksAndMessages(PING_TOKEN)
        s.pingAll({ bump() }, { pingingSlots.remove(s.id()); bump(); scheduleHidePing() })
    }

    /** Пинг одного сервера (свайп по строке). */
    fun pingOne(i: Int, s: XKStore = st) {
        val key = s.id() * 1000 + i
        if (!s.hasSubscription() || key in pingingOne) return
        pingingOne.add(key); pingHidden = false
        s.pingSome(intArrayOf(i), { bump() }, { pingingOne.remove(key); bump(); scheduleHidePing() })
    }

    private fun scheduleHidePing() {
        handler.removeCallbacksAndMessages(PING_TOKEN)
        val keep = st.pref(XKStore.P_PING_KEEP, 60)
        if (keep > 0) handler.postAtTime({ pingHidden = true; bump() }, PING_TOKEN, android.os.SystemClock.uptimeMillis() + keep * 1000L)
    }

    /** Пинг сервера: null — не показывать, -1 нет ответа, -2 UDP-протокол. */
    fun pingOf(i: Int, s: XKStore = st): Int? { rev; return if (pingHidden && !isPinging(s)) null else s.pings[i] }

    // ---------- подключение ----------
    fun startVpn() { XKVpnService.start(getApplication()) }
    fun stopVpn() { XKVpnService.stop(getApplication()) }

    fun reconnectIfNeeded() {
        if (XKVpnService.state == XKVpnService.IDLE) return
        stopVpn()
        handler.postDelayed({ if (XKVpnService.state == XKVpnService.IDLE) startVpn() }, 1200)
    }

    // ---------- настройки ----------
    fun setFlag(key: String, value: Boolean, reconnect: Boolean = false) {
        st.prefs().edit().putBoolean(key, value).apply()
        if (key == XKStore.P_REMIND) st.scheduleReminders()
        bump(); if (reconnect) reconnectIfNeeded()
    }
    fun setPref(key: String, value: Int, reconnect: Boolean = false) {
        st.prefs().edit().putInt(key, value).apply()
        bump(); if (reconnect) reconnectIfNeeded()
    }
    fun flag(key: String, def: Boolean): Boolean { rev; return st.flag(key, def) }
    fun pref(key: String, def: Int): Int { rev; return st.pref(key, def) }

    fun setTheme(mode: Int) { themeMode = mode; st.prefs().edit().putInt(XKStore.P_THEME, mode).apply() }
    fun choosePalette(p: Int) { palette = p; dynamicColors = false; st.prefs().edit().putInt("palette", p).putBoolean("dynamic", false).apply() }
    fun setDynamic(on: Boolean) { dynamicColors = on; st.prefs().edit().putBoolean("dynamic", on).apply() }
    fun chooseAmoled(on: Boolean) { amoled = on; st.prefs().edit().putBoolean("amoled", on).apply() }

    fun setBypassCats(s: Set<String>) { st.setBypassCats(s); bump(); reconnectIfNeeded() }
    fun setBypassSites(s: Collection<String>) { st.setBypassSites(s); bump(); reconnectIfNeeded() }
    fun setExcludedApps(s: Set<String>) { st.setExcludedApps(s); bump(); reconnectIfNeeded() }

    companion object { private val PING_TOKEN = Any() }
}
