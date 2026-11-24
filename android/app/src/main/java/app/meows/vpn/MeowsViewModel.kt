package app.meows.vpn

import app.meows.vpn.i18n.trf
import app.meows.vpn.i18n.tr
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
import kotlinx.coroutines.withContext
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
    var dynamicColors by mutableStateOf(st.flag("dynamic", false))
    var amoled by mutableStateOf(st.flag("amoled", false))

    private var autoPingDone = false

    /** Настройка, которая сейчас применяется переподключением VPN (её строка показывает индикатор); null — ничего не применяется. */
    var applyingKey by mutableStateOf<String?>(null); private set
    private var applyPhase = 0   // 0 — нет, 1 — VPN останавливается, 2 — запускается заново
    fun isApplying(key: String) = applyingKey == key

    init {
        XKAppHolder.ctx = app.applicationContext
        XKVpnService.addListener(this)
        st.scheduleReminders()
        if (!Edition.isPlay) updates.checkOnLaunch()
        if (st.url().isNotEmpty() && (st.updateOnLaunch() || st.needsAutoUpdate())) refresh(true)
        else autoPing()
    }

    override fun onCleared() {
        XKVpnService.removeListener(this)
        super.onCleared()
    }

    override fun onState(state: Int) {
        vpnState = state
        if (applyPhase == 2) { if (state == XKVpnService.CONNECTED) finishApply(true) else if (state == XKVpnService.IDLE) finishApply(false) }
        bump()   // сервис мог сам сменить сервер (автопереключение) — обновляем список и выбранный сервер
        if (state == XKVpnService.IDLE) {
            XKVpnService.lastError?.let { alert(tr("Не подключилось"), it); XKVpnService.lastError = null }
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
        if (error.contains("лимит", ignoreCase = true) || error.contains("limit", ignoreCase = true)) {
            val cabinet = app.meows.vpn.shop.ShopLinks.cabinet(st)
            if (cabinet != null) alert(title, error, tr("Открыть кабинет")) { openLink(cabinet) } else alert(title, error)
        } else alert(title, error)
    }
    fun bump() { rev++ }

    // ---------- подписки ----------
    /** Все подписки (карточки на главной). */
    val subs: List<XKStore> get() { rev; return XKStore.all(getApplication()).filter { it.hasSubscription() }.sortedByDescending { it.isPinned() } }   // закреплённые — сверху
    val servers: List<XKStore.Server> get() { rev; return st.servers() }
    val selected: Int get() { rev; return st.index() }
    val hasSub: Boolean get() { rev; return XKStore.all(getApplication()).any { it.hasSubscription() } }

    /** Добавить подписку: такая же ссылка уже есть — обновим её, иначе новая карточка. Первая подписка становится активной. */
    fun addSubscription(raw: String) {
        val link = LinkNormalizer.normalize(raw)
        if (!LinkNormalizer.isHttpLink(link)) { fetchError(tr("Не получилось"), tr("Это не похоже на ссылку подписки.")); return }
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
                bump(); fetchError(tr("Не получилось"), error); return@fetch
            }
            onOk()
            toast = if (replaced) tr("Ссылка заменена") else tr("Подписка добавлена")
            if (isActive(s)) { syncGeo(); bump(); autoPing(); reconnectIfNeeded() } else bump()
        }
    }

    fun togglePin(s: XKStore) { s.setPinned(!s.isPinned()); bump() }

    fun refresh(silent: Boolean) = refresh(st, silent)

    fun refresh(s: XKStore, silent: Boolean) {
        if (s.url().isEmpty() || s.id() in refreshingSlots) return
        refreshingSlots.add(s.id())
        s.fetch(s.url()) { error ->
            refreshingSlots.remove(s.id())
            if (error != null) { if (!silent) fetchError(tr("Не удалось обновить"), error); return@fetch }
            if (!silent) toast = tr("Подписка обновлена")
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
        if (t.startsWith("http", true) || t.contains("://add/")) addSubscription(t) else toast = tr("В буфере нет ссылки подписки")
    }

    // ---------- сервер / пинг ----------
    fun select(i: Int) = select(st, i)

    /** Выбор сервера в любой карточке: подписка карточки становится активной. */
    fun select(s: XKStore, i: Int) {
        if (isActive(s) && i == s.index()) return
        MobileBypass.userPicked(getApplication())
        XKStore.activate(getApplication(), s.id()); s.setIndex(i); bump(); reconnectIfNeeded()
    }

    /** «Выбрать быстрейший»: пингуем все серверы подписки и выбираем лучший по результату. */
    fun selectFastest(s: XKStore = st) {
        if (!s.hasSubscription() || isPinging(s)) return
        toast = tr("Измеряем пинг…")
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
        if (best < 0) { toast = tr("Ни один сервер не ответил"); return }
        toast = trf("Быстрейший: %s · %d мс", list[best].title, bestMs)
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

    /** Переподключить VPN, если он включён. [applyKey] — настройка, ради которой это делаем: её строка покажет «Применяем…», а в конце придёт сообщение. */
    fun reconnectIfNeeded(applyKey: String? = null) {
        if (XKVpnService.state == XKVpnService.IDLE) return
        if (applyKey != null) {
            applyingKey = applyKey; applyPhase = 1
            handler.removeCallbacksAndMessages(APPLY_TOKEN)
            handler.postAtTime({ finishApply(false) }, APPLY_TOKEN, android.os.SystemClock.uptimeMillis() + 30_000)   // страховка: индикатор не должен зависнуть
        }
        stopVpn()
        handler.postDelayed({
            if (XKVpnService.state == XKVpnService.IDLE) { if (applyPhase == 1) applyPhase = 2; startVpn() }
            else finishApply(false)
        }, 1200)
    }

    private fun finishApply(ok: Boolean) {
        if (applyPhase == 0) return
        applyPhase = 0; applyingKey = null
        handler.removeCallbacksAndMessages(APPLY_TOKEN)
        if (ok) toast = tr("Настройка применена. VPN переподключился")
    }

    // ---------- настройки ----------
    fun setFlag(key: String, value: Boolean, reconnect: Boolean = false) {
        st.prefs().edit().putBoolean(key, value).apply()
        if (key == XKStore.P_REMIND) st.scheduleReminders()
        bump(); if (reconnect) reconnectIfNeeded(key)
    }
    fun setPref(key: String, value: Int, reconnect: Boolean = false) {
        st.prefs().edit().putInt(key, value).apply()
        bump(); if (reconnect) reconnectIfNeeded(key)
    }
    /** Выбор профиля маршрутизации; применяется переподключением. */
    fun setRouteProfile(id: String) {
        st.prefs().edit().putString(XKStore.P_ROUTE_PROFILE, id).apply()
        bump(); applyRouteChange()
    }

    /**
     * Профиль маршрутизации выбран или изменён: сначала приводим гео-файлы к его ссылкам (иначе правила с новыми тегами
     * не сработают), затем переподключаем VPN, если он включён.
     */
    fun applyRouteChange() {
        if (XKVpnService.state == XKVpnService.IDLE) { syncGeo(); return }
        applyingKey = XKStore.P_ROUTE_PROFILE; applyPhase = 1
        viewModelScope.launch {
            withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { app.meows.vpn.geo.GeoUpdater.sync(getApplication(), st) } }
            if (XKVpnService.state == XKVpnService.IDLE) finishApply(false)   // пока качали, VPN выключили
            else reconnectIfNeeded(XKStore.P_ROUTE_PROFILE)
        }
    }

    /** Кнопка «Обновить» в профиле: скачать гео-файлы по [pins] (null — по ссылкам подписки или вшитые). После обновления VPN переподключается. */
    suspend fun updateGeo(pins: app.meows.vpn.geo.GeoUpdater.Pins?): app.meows.vpn.geo.GeoUpdater.Result {
        val r = withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { app.meows.vpn.geo.GeoUpdater.sync(getApplication(), st, pins ?: app.meows.vpn.geo.GeoUpdater.parsePins(st.routingHeader()), force = true) }
                .getOrDefault(app.meows.vpn.geo.GeoUpdater.Result.FAILED)
        }
        bump()
        if (r == app.meows.vpn.geo.GeoUpdater.Result.UPDATED || r == app.meows.vpn.geo.GeoUpdater.Result.BUNDLED) reconnectIfNeeded()
        return r
    }

    fun geoStatus() = app.meows.vpn.geo.GeoUpdater.status(getApplication(), st)
    fun flag(key: String, def: Boolean): Boolean { rev; return st.flag(key, def) }
    fun pref(key: String, def: Int): Int { rev; return st.pref(key, def) }

    fun setTheme(mode: Int) { themeMode = mode; st.prefs().edit().putInt(XKStore.P_THEME, mode).apply() }
    fun choosePalette(p: Int) { palette = p; dynamicColors = false; st.prefs().edit().putInt("palette", p).putBoolean("dynamic", false).apply() }
    fun setDynamic(on: Boolean) { dynamicColors = on; st.prefs().edit().putBoolean("dynamic", on).apply() }
    fun chooseAmoled(on: Boolean) { amoled = on; st.prefs().edit().putBoolean("amoled", on).apply() }

    fun setSwitchMode(m: Int) { SwitchFilter.setMode(st.prefs(), m); bump() }
    fun toggleSwitchTarget(title: String) { SwitchFilter.toggle(st.prefs(), st.servers(), title); bump() }

    /** «Обход в мобильной сети»: включение, выбор серверов обхода (по ключу «слот|название»). */
    fun setMobileBypass(on: Boolean) {
        val ctx = getApplication<Application>()
        st.prefs().edit().putBoolean(MobileBypass.P_ON, on).apply()
        bump()
        if (on) { MobileBypassWatcher.recheck(); return }
        if (MobileBypass.disable(ctx) != null) { bump(); reconnectIfNeeded() }   // выключили во время обхода — возвращаем прежний сервер
    }
    fun toggleBypassServer(key: String) { MobileBypass.toggle(getApplication(), key); bump() }

    fun setBypassCats(s: Set<String>) { st.setBypassCats(s); bump(); reconnectIfNeeded("bypass") }
    fun setBypassSites(s: Collection<String>) { st.setBypassSites(s); bump(); reconnectIfNeeded("bypass") }
    fun setExcludedApps(s: Set<String>) { st.setExcludedApps(s); bump(); reconnectIfNeeded("bypass") }

    companion object { private val PING_TOKEN = Any(); private val APPLY_TOKEN = Any() }
}
