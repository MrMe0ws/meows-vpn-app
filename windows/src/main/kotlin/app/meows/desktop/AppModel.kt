package app.meows.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.meows.desktop.data.SettingsStore
import app.meows.desktop.data.SubscriptionClient
import app.meows.desktop.data.SubscriptionData
import app.meows.desktop.vpn.CoreProcess
import app.meows.desktop.vpn.PingRunner
import app.meows.desktop.vpn.SystemProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class ConnState { Idle, Connecting, Connected }

object AppInfo { const val VERSION = "1.0.0" }

/** Состояние и команды приложения: подписка, серверы, подключение (ядро + системный прокси), пинг, настройки. */
class AppModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settings = SettingsStore()
    private val core = CoreProcess()
    private val proxy = SystemProxy(settings)

    var sub by mutableStateOf(SubscriptionData.fromJson(settings.str("sub")))
    var selected by mutableIntStateOf(settings.int("selected", 0))
    var state by mutableStateOf(ConnState.Idle)
    var connectedAt by mutableLongStateOf(0)
    var refreshing by mutableStateOf(false)
    var pinging by mutableStateOf(false)
    val pings = mutableStateMapOf<Int, Int>()
    var error by mutableStateOf<Pair<String, String>?>(null)
    var toast by mutableStateOf<String?>(null)

    var themeMode by mutableIntStateOf(settings.int("theme", 0))       // 0 авто, 1 светлая, 2 тёмная
    var palette by mutableIntStateOf(settings.int("palette", 0))
    var amoled by mutableStateOf(settings.bool("amoled", false))
    var blockAds by mutableStateOf(settings.bool("blockAds", false))
    var ruDirect by mutableStateOf(settings.bool("ruDirect", false))

    init {
        proxy.restore()   // прошлый запуск мог закончиться аварийно с включённым прокси
        if (sub != null && System.currentTimeMillis() - (sub?.updated ?: 0) > 60_000) refresh(silent = true)
    }

    private fun opts() = JSONObject().put("ruDirect", ruDirect).put("blockAds", blockAds).put("noVV", true).toString()

    fun addSubscription(raw: String) = fetch(raw, silent = false, added = true)
    fun refresh(silent: Boolean) { sub?.url?.let { fetch(it, silent, false) } }

    private fun fetch(raw: String, silent: Boolean, added: Boolean) {
        if (refreshing) return
        refreshing = true
        scope.launch {
            SubscriptionClient.fetch(raw, AppInfo.VERSION).onSuccess {
                sub = it; settings.set("sub", it.toJson().toString()); pings.clear()
                if (selected >= it.servers.size) select(0)
                if (!silent) toast = if (added) "Подписка добавлена" else "Подписка обновлена"
                if (state == ConnState.Connected && added) reconnect()
            }.onFailure { if (!silent) error = (if (added) "Не получилось" else "Не удалось обновить") to (it.message ?: "Ошибка") }
            refreshing = false
        }
    }

    fun deleteSubscription() { disconnect(); sub = null; settings.set("sub", null); pings.clear() }

    fun select(i: Int) {
        if (i == selected) return
        selected = i; settings.set("selected", i)
        if (state != ConnState.Idle) reconnect()
    }

    fun toggle() {
        when (state) {
            ConnState.Idle -> if (sub == null) error = "Сначала подписка" to "Добавьте ссылку подписки, чтобы подключиться." else connect()
            else -> disconnect()
        }
    }

    fun connect() {
        val s = sub ?: return
        state = ConnState.Connecting
        scope.launch {
            core.start(s, selected, opts()) { tail ->
                proxy.restore(); state = ConnState.Idle; connectedAt = 0
                error = "Подключение прервано" to (tail ?: "Ядро остановилось")
            }.onSuccess {
                runCatching { proxy.enable(CoreProcess.HTTP_PORT) }.onFailure { error = "Системный прокси" to (it.message ?: "Не удалось включить") }
                connectedAt = System.currentTimeMillis(); state = ConnState.Connected
            }.onFailure { state = ConnState.Idle; error = "Не подключилось" to (it.message ?: "Ошибка ядра") }
        }
    }

    fun disconnect() {
        scope.launch { proxy.restore(); core.stop(); state = ConnState.Idle; connectedAt = 0 }
    }

    private fun reconnect() { scope.launch { proxy.restore(); core.stop(); connect() } }

    fun pingAll() {
        val s = sub ?: return
        if (pinging) return
        pinging = true; pings.clear()
        scope.launch {
            PingRunner.measure(s.body, s.servers.indices.toList()) { i, ms -> pings[i] = ms }
            pinging = false
        }
    }

    fun setTheme(m: Int) { themeMode = m; settings.set("theme", m) }
    fun choosePalette(p: Int) { palette = p; settings.set("palette", p) }
    fun chooseAmoled(v: Boolean) { amoled = v; settings.set("amoled", v) }
    fun chooseBlockAds(v: Boolean) { blockAds = v; settings.set("blockAds", v); if (state != ConnState.Idle) reconnect() }
    fun chooseRuDirect(v: Boolean) { ruDirect = v; settings.set("ruDirect", v); if (state != ConnState.Idle) reconnect() }

    fun shutdown() { proxy.restore(); core.stop() }
}