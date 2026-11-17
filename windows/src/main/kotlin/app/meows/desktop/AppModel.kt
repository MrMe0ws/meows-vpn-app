package app.meows.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.meows.desktop.data.LaunchLog
import app.meows.desktop.data.LinkNormalizer
import app.meows.desktop.data.SettingsStore
import app.meows.desktop.data.SubscriptionClient
import app.meows.desktop.data.SubscriptionData
import app.meows.desktop.log.AppLog
import app.meows.desktop.notify.Reminders
import app.meows.desktop.stats.StatsFeed
import app.meows.desktop.system.Autostart
import app.meows.desktop.system.Elevation
import app.meows.desktop.update.UpdateController
import app.meows.desktop.vpn.CoreProcess
import app.meows.desktop.vpn.FailoverWatch
import app.meows.desktop.vpn.PingRunner
import app.meows.desktop.vpn.Probe
import app.meows.desktop.vpn.SystemProxy
import app.meows.desktop.vpn.VpnMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class ConnState { Idle, Connecting, Connected }

/** Версию задаёт jpackage (packageVersion в build.gradle.kts); при запуске из Gradle — запасное значение. */
object AppInfo { val VERSION: String = System.getProperty("jpackage.app-version") ?: "1.0.0" }

/** Состояние и команды приложения: подписка, серверы, подключение (ядро + системный прокси), пинг, настройки. */
class AppModel(startHidden: Boolean = false, connectOnStart: Boolean = false) {
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
    val pingBusy = mutableStateListOf<Int>()           // серверы, у которых сейчас идёт пинг
    val pingingSelected: Boolean get() = selected in pingBusy
    var mode by mutableStateOf(VpnMode.of(settings.str("mode", "tun")))
    val pings = mutableStateMapOf<Int, Int>()
    var error by mutableStateOf<Pair<String, String>?>(null)
    var toast by mutableStateOf<String?>(null)

    var trayAnchor by mutableStateOf<java.awt.Point?>(null)   // где показать меню трея (null — скрыто)
    var windowVisible by mutableStateOf(!startHidden)
    var showTick by mutableIntStateOf(0)            // растёт, когда окно нужно вынести на передний план
    var exitRequested by mutableStateOf(false)
    var closeToTray by mutableStateOf(settings.bool("closeToTray", true))
    var autoStart by mutableStateOf(Autostart.isEnabled())
    var autoConnect by mutableStateOf(settings.bool("autoConnect", false))
    val updates = UpdateController(settings, scope)

    var themeMode by mutableIntStateOf(settings.int("theme", 0))       // 0 авто, 1 светлая, 2 тёмная
    var palette by mutableIntStateOf(settings.int("palette", 10))
    var amoled by mutableStateOf(settings.bool("amoled", false))
    var blockAds by mutableStateOf(settings.bool("blockAds", false))
    var ruDirect by mutableStateOf(settings.bool("ruDirect", false))
    var railExpanded by mutableStateOf(settings.bool("railExpanded", true))   // боковое меню: с подписями / только иконки
    var autoSwitch by mutableStateOf(settings.bool("autoSwitch", true))   // сменить сервер, если текущий перестал отвечать
    var remind by mutableStateOf(settings.bool("remind", true))           // напоминания о сроке и трафике
    var notifier: ((String, String, String?) -> Unit)? = null             // всплывающее уведомление (ставит трей)
    private val switchTimes = ArrayDeque<Long>()
    private val watch = FailoverWatch(scope, { autoSwitch }, { state == ConnState.Connected },
        { Probe.viaVpn(if (mode == VpnMode.Proxy) CoreProcess.HTTP_PORT else null) }) { failover() }

    init {
        AppLog.add("APP", "Meows ${AppInfo.VERSION} · Windows ${System.getProperty("os.version")} · администратор: ${if (Elevation.isAdmin()) "да" else "нет"} · режим: ${mode.title}")
        proxy.restore()   // прошлый запуск мог закончиться аварийно с включённым прокси
        if (sub != null && System.currentTimeMillis() - (sub?.updated ?: 0) > 60_000) refresh(silent = true)
        updates.checkOnLaunch()
        if ((autoConnect || connectOnStart) && sub != null) connect()
        scope.launch { delay(8_000); while (true) { checkReminders(); delay(6 * 3_600_000L) } }
    }

    /** Напоминания: срок подписки (3 дня / 1 день / закончилась) и трафик (90 % / 100 %); каждое — один раз на срок подписки. */
    fun checkReminders() {
        val s = sub ?: return
        if (!remind) return
        for (n in Reminders.due(s)) {
            if (settings.str("remind.${n.kind}") == n.key) continue
            settings.set("remind.${n.kind}", n.key)
            notifier?.invoke(n.title, n.text, s.webUrl ?: "https://cabinet.me0ws.store")
            AppLog.add("APP", "Напоминание: ${n.title}")
        }
    }

    /** Сообщение от второго запуска: ссылка meows://add/<подписка> или «SHOW» — показать окно. */
    fun handleExternal(msg: String) {
        if (msg == "TRAYMENU") { trayAnchor = java.awt.MouseInfo.getPointerInfo().location; return }   // проверка меню без клика по трею
        LaunchLog.write("первый экземпляр получил: [$msg]")
        if (msg.startsWith("meows:", true) || msg.startsWith("happ:", true)) addSubscription(msg)
        windowVisible = true; showTick++
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
                AppLog.add("SUB", (if (added) "Подписка добавлена" else "Подписка обновлена") + ": серверов ${it.servers.size}")
                if (selected >= it.servers.size) select(0)
                if (!silent) toast = if (added) "Подписка добавлена" else "Подписка обновлена"
                checkReminders()
                if (state == ConnState.Connected && added) reconnect()
            }.onFailure {
                AppLog.add("SUB", "Ошибка: ${it.message}" + if (it.message?.startsWith("Это не похоже") == true) " (получено: ${LinkNormalizer.head(raw)})" else "")
                if (!silent) error = (if (added) "Не получилось" else "Не удалось обновить") to (it.message ?: "Ошибка") }
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
        if (mode == VpnMode.Tun && !Elevation.isAdmin()) { elevateAndConnect(); return }
        state = ConnState.Connecting
        StatsFeed.reset()
        AppLog.add("VPN", "Подключаемся: ${s.servers.getOrNull(selected)?.title ?: "сервер $selected"}, режим «${mode.title}»")
        scope.launch {
            core.start(s, selected, opts(), mode.key) { tail ->
                proxy.restore(); state = ConnState.Idle; connectedAt = 0
                error = "Подключение прервано" to (tail ?: "Ядро остановилось")
            }.onSuccess {
                if (mode == VpnMode.Proxy) runCatching { proxy.enable(CoreProcess.HTTP_PORT) }.onFailure { error = "Системный прокси" to (it.message ?: "Не удалось включить") }
                connectedAt = System.currentTimeMillis(); state = ConnState.Connected; AppLog.add("VPN", "Подключено"); watch.start()
            }.onFailure { state = ConnState.Idle; AppLog.add("VPN", "Не подключилось: ${it.message}"); error = "Не подключилось" to (it.message ?: "Ошибка ядра") }
        }
    }

    /** TUN требует администратора: перезапускаемся с правами (окно UAC) и подключаемся уже там. */
    private fun elevateAndConnect() {
        if (Elevation.relaunch(listOf("--connect"))) { shutdown(); exitRequested = true }
        else error = "Нужны права администратора" to "Режим «Все программы» создаёт виртуальную сетевую карту — подтвердите запрос Windows. Или выберите «Системный прокси»: он работает без администратора."
    }

    fun chooseMode(m: VpnMode) {
        if (m == mode) return
        val was = state != ConnState.Idle
        if (was) { proxy.restore(); core.stop(); state = ConnState.Idle; connectedAt = 0 }
        mode = m; settings.set("mode", m.key)
        if (was) connect()
    }

    fun disconnect() {
        watch.stop()
        scope.launch { proxy.restore(); core.stop(); state = ConnState.Idle; connectedAt = 0; AppLog.add("VPN", "Отключено") }
    }

    private fun reconnect() { watch.stop(); scope.launch { proxy.restore(); core.stop(); connect() } }

    fun pingAll() {
        val s = sub ?: return
        if (pinging) return
        pinging = true; pings.clear()
        scope.launch {
            PingRunner.measure(s.body, s.servers.indices.toList()) { i, ms -> pings[i] = ms }
            pinging = false
        }
    }

    /** «Выбрать быстрейший»: пингуем все серверы и выбираем лучший по результату. */
    fun selectFastest() {
        val s = sub ?: return
        if (pinging) return
        toast = "Измеряем пинг…"; pinging = true; pings.clear()
        scope.launch {
            PingRunner.measure(s.body, s.servers.indices.toList()) { i, ms -> pings[i] = ms }
            pinging = false
            val best = pings.filter { it.value > 0 }.minByOrNull { it.value }
            if (best == null) toast = "Ни один сервер не ответил"
            else { toast = "Быстрейший: ${s.servers[best.key].title} · ${best.value} мс"; select(best.key) }
        }
    }

    /**
     * Связь через VPN пропала (сторож). Снимаем туннель, чтобы измерить серверы по обычной сети. Текущий не отвечает, а другой жив —
     * переключаемся на лучший (не чаще 3 раз за 10 минут). Никто не отвечает — похоже, нет интернета: переподключаемся как было.
     */
    private suspend fun failover() {
        val s = sub ?: return
        val cur = selected
        AppLog.add("VPN", "Связь через «${s.servers.getOrNull(cur)?.title}» пропала — проверяем серверы")
        proxy.restore(); core.stop(); state = ConnState.Connecting; connectedAt = 0
        val res = java.util.concurrent.ConcurrentHashMap<Int, Int>()
        PingRunner.measure(s.body, s.servers.indices.toList()) { i, ms -> res[i] = ms; pings[i] = ms }
        val best = res.filter { it.key != cur && it.value > 0 }.minByOrNull { it.value }?.key
        val now = System.currentTimeMillis()
        while (switchTimes.isNotEmpty() && now - switchTimes.first() > 600_000L) switchTimes.removeFirst()
        if (best != null && (res[cur] ?: -1) <= 0 && switchTimes.size < 3) {
            switchTimes.addLast(now)
            val msg = "«${s.servers.getOrNull(cur)?.title}» не отвечал — подключились к «${s.servers[best].title}»"
            selected = best; settings.set("selected", best)
            AppLog.add("VPN", "Сервер сменился: $msg"); toast = msg; notifier?.invoke("Сервер сменился", msg, null)
        } else AppLog.add("VPN", if (res.values.none { it > 0 }) "Ни один сервер не отвечает — похоже, нет интернета, переподключаемся" else "Сервер отвечает, переподключаемся")
        connect()
    }

    /** Пинг одного сервера (правая кнопка по серверу и кнопка справа). */
    fun pingServer(i: Int) {
        val s = sub ?: return
        if (i in pingBusy) return
        pingBusy.add(i); pings.remove(i)
        scope.launch { PingRunner.measure(s.body, listOf(i)) { k, ms -> pings[k] = ms }; pingBusy.remove(i) }
    }

    fun setTheme(m: Int) { themeMode = m; settings.set("theme", m) }
    fun choosePalette(p: Int) { palette = p; settings.set("palette", p) }
    fun chooseAmoled(v: Boolean) { amoled = v; settings.set("amoled", v) }
    fun chooseBlockAds(v: Boolean) { blockAds = v; settings.set("blockAds", v); if (state != ConnState.Idle) reconnect() }
    fun chooseRuDirect(v: Boolean) { ruDirect = v; settings.set("ruDirect", v); if (state != ConnState.Idle) reconnect() }

    fun chooseCloseToTray(v: Boolean) { closeToTray = v; settings.set("closeToTray", v) }
    fun chooseAutoStart(v: Boolean) { Autostart.set(v); autoStart = Autostart.isEnabled() }
    fun toggleRail() { railExpanded = !railExpanded; settings.set("railExpanded", railExpanded) }
    fun chooseAutoSwitch(v: Boolean) { autoSwitch = v; settings.set("autoSwitch", v) }
    fun chooseRemind(v: Boolean) { remind = v; settings.set("remind", v); if (v) checkReminders() }
    fun chooseAutoConnect(v: Boolean) { autoConnect = v; settings.set("autoConnect", v) }

    /** Скачанное обновление: запускаем установщик и закрываемся (он сам перезапустит приложение). */
    fun installUpdate() { if (updates.launchInstaller()) { shutdown(); exitRequested = true } }

    fun shutdown() { watch.stop(); proxy.restore(); core.stop() }
}