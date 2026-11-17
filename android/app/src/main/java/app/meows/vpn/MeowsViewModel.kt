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
    val st: XKStore = XKStore.get(app)
    private val handler = Handler(Looper.getMainLooper())

    var vpnState by mutableIntStateOf(XKVpnService.state); private set
    /** Растёт при любом изменении подписки, серверов, пингов или настроек. */
    var rev by mutableIntStateOf(0); private set
    var refreshing by mutableStateOf(false); private set
    var pinging by mutableStateOf(false); private set
    /** Серверы, которые сейчас пингуются по одному (свайп). */
    val pingingOne = mutableStateSetOf<Int>()
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

    // ---------- подписка ----------
    val servers: List<XKStore.Server> get() { rev; return st.servers() }
    val selected: Int get() { rev; return st.index() }
    val hasSub: Boolean get() { rev; return st.hasSubscription() }

    fun addSubscription(raw: String) {
        refreshing = true
        st.fetch(raw) { error ->
            refreshing = false
            if (error != null) { fetchError("Не получилось", error); return@fetch }
            toast = "Подписка добавлена"
            syncGeo()
            bump(); autoPing(); reconnectIfNeeded()
        }
    }

    fun refresh(silent: Boolean) {
        if (st.url().isEmpty() || refreshing) return
        refreshing = true
        st.fetch(st.url()) { error ->
            refreshing = false
            if (error != null) { if (!silent) fetchError("Не удалось обновить", error); return@fetch }
            if (!silent) toast = "Подписка обновлена"
            syncGeo()
            bump(); autoPing()
        }
    }

    /** Гео-файлы следуют ссылкам из заголовка `routing` панели (см. GeoUpdater). */
    private fun syncGeo() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { app.meows.vpn.geo.GeoUpdater.sync(getApplication(), st) } }
    }

    fun deleteSubscription() { st.clear(); bump(); if (vpnState != XKVpnService.IDLE) XKVpnService.stop(getApplication()) }

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
    fun select(i: Int) {
        if (i == st.index()) return
        st.setIndex(i); bump(); reconnectIfNeeded()
    }

    fun selectFastest() {
        var best = -1; var bestMs = Int.MAX_VALUE
        for (i in servers.indices) {
            val ms = st.pings[i] ?: continue
            if (ms in 1 until bestMs) { bestMs = ms; best = i }
        }
        if (best >= 0) select(best) else toast = "Сначала измерьте пинг"
    }

    private fun autoPing() {
        if (autoPingDone || !st.flag(XKStore.P_PING_LAUNCH, false) || !st.hasSubscription()) return
        autoPingDone = true; ping()
    }

    fun ping() {
        if (pinging || !st.hasSubscription()) return
        pinging = true; pingHidden = false
        handler.removeCallbacksAndMessages(PING_TOKEN)
        st.pingAll({ bump() }, { pinging = false; bump(); scheduleHidePing() })
    }

    /** Пинг одного сервера (свайп по строке). */
    fun pingOne(i: Int) {
        if (!st.hasSubscription() || i in pingingOne) return
        pingingOne.add(i); pingHidden = false
        st.pingSome(intArrayOf(i), { bump() }, { pingingOne.remove(i); bump(); scheduleHidePing() })
    }

    private fun scheduleHidePing() {
        handler.removeCallbacksAndMessages(PING_TOKEN)
        val keep = st.pref(XKStore.P_PING_KEEP, 60)
        if (keep > 0) handler.postAtTime({ pingHidden = true; bump() }, PING_TOKEN, android.os.SystemClock.uptimeMillis() + keep * 1000L)
    }

    /** Пинг сервера: null — не показывать, -1 нет ответа, -2 UDP-протокол. */
    fun pingOf(i: Int): Int? { rev; return if (pingHidden && !pinging) null else st.pings[i] }

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
