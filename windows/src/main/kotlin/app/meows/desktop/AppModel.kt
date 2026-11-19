package app.meows.desktop

import app.meows.desktop.i18n.Lang
import app.meows.desktop.i18n.trf
import app.meows.desktop.i18n.tr
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
import app.meows.desktop.data.SubEntry
import app.meows.desktop.data.SwitchFilter
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

    /** Подписки (карточки). Активная — с ней подключается VPN: выбор сервера в любой карточке делает её активной. */
    val subs = mutableStateListOf<SubEntry>().apply { addAll(SubEntry.load(settings)) }
    private var activeId by mutableIntStateOf(settings.int("activeSub", -1))
    val active: SubEntry? get() = subs.firstOrNull { it.id == activeId } ?: subs.firstOrNull()
    /** Для показа: закреплённые сверху, остальные в порядке добавления. */
    val ordered: List<SubEntry> get() = subs.sortedByDescending { it.pinned }
    fun isActive(e: SubEntry) = e.id == active?.id
    val sub: SubscriptionData? get() = active?.data
    var selected: Int
        get() = active?.selected ?: 0
        set(v) { active?.selected = v; saveSubs() }
    var state by mutableStateOf(ConnState.Idle)
    var connectedAt by mutableLongStateOf(0)
    val pingingSelected: Boolean get() = selected in (active?.pingBusy ?: emptyList<Int>())
    var mode by mutableStateOf(VpnMode.of(settings.str("mode", "tun")))
    /** Результаты пинга активной подписки (для панели справа). */
    val pings: Map<Int, Int> get() = active?.pings ?: emptyMap()
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
    var palette by mutableIntStateOf(settings.int("palette", 0))
    var amoled by mutableStateOf(settings.bool("amoled", false))
    var blockAds by mutableStateOf(settings.bool("blockAds", false))
    var ruDirect by mutableStateOf(settings.bool("ruDirect", false))
    var allVpn by mutableStateOf(settings.bool("allVpn", false))          // весь трафик через VPN, кроме торрентов и локальной сети
    var railExpanded by mutableStateOf(settings.bool("railExpanded", true))   // боковое меню: с подписями / только иконки
    var autoSwitch by mutableStateOf(settings.bool("autoSwitch", true))   // сменить сервер, если текущий перестал отвечать
    val switchFilter = SwitchFilter(settings)                              // на какие серверы можно переключаться
    var remind by mutableStateOf(settings.bool("remind", true))         // напоминания о сроке и трафике
    var notifier: ((String, String, String?) -> Unit)? = null             // всплывающее уведомление (ставит трей)
    private val switchTimes = ArrayDeque<Long>()
    private val watch = FailoverWatch(scope, { autoSwitch }, { state == ConnState.Connected },
        { Probe.viaVpn(if (mode == VpnMode.Proxy) CoreProcess.HTTP_PORT else null) }) { failover() }

    init {
        Lang.init(settings)
        AppLog.add("APP", "Meows ${AppInfo.VERSION} · Windows ${System.getProperty("os.version")} · администратор: ${if (Elevation.isAdmin()) "да" else "нет"} · режим: ${mode.title}")
        proxy.restore()   // прошлый запуск мог закончиться аварийно с включённым прокси
        subs.forEach { if (System.currentTimeMillis() - it.data.updated > 60_000) refresh(it, silent = true) }
        updates.checkOnLaunch()
        if ((autoConnect || connectOnStart) && sub != null) connect()
        scope.launch { delay(8_000); while (true) { checkReminders(); delay(6 * 3_600_000L) } }
    }

    /** Напоминания: срок подписки (3 дня / 1 день / закончилась) и трафик (90 % / 100 %); каждое — один раз на срок подписки, у каждой подписки свои. */
    fun checkReminders() {
        if (!remind) return
        for (e in subs.toList()) {
            val d = e.data
            for (n in Reminders.due(d)) {
                val key = "remind.${n.kind}.${e.id}"
                if (settings.str(key) == n.key) continue
                settings.set(key, n.key)
                val who = if (subs.size > 1) "«${d.title}»: " else ""
                notifier?.invoke(n.title, who + n.text, d.webUrl ?: "https://cabinet.me0ws.store")
                AppLog.add("APP", "Напоминание: ${n.title}" + if (subs.size > 1) " (${d.title})" else "")
            }
        }
    }

    /** Сообщение от второго запуска: ссылка meows://add/<подписка> или «SHOW» — показать окно. */
    fun handleExternal(msg: String) {
        if (msg == "TRAYMENU") { trayAnchor = java.awt.MouseInfo.getPointerInfo().location; return }   // проверка меню без клика по трею
        LaunchLog.write("первый экземпляр получил: [$msg]")
        if (msg.startsWith("meows:", true) || msg.startsWith("happ:", true)) addSubscription(msg)
        windowVisible = true; showTick++
    }

    private fun opts() = JSONObject().put("ruDirect", ruDirect).put("blockAds", blockAds).put("allVpn", allVpn).put("noVV", true).toString()

    private fun saveSubs() = SubEntry.save(settings, subs, active?.id ?: -1)

    /** Новая ссылка — новая карточка; такая же ссылка уже есть — обновляем её. Первая подписка становится активной. */
    fun addSubscription(raw: String) {
        val link = LinkNormalizer.normalize(raw)
        fetch(raw, subs.firstOrNull { it.data.url == link }, silent = false)
    }

    fun refresh(e: SubEntry, silent: Boolean) = fetch(e.data.url, e, silent)

    private fun fetch(raw: String, existing: SubEntry?, silent: Boolean) {
        if (existing?.refreshing == true) return
        existing?.refreshing = true
        scope.launch {
            SubscriptionClient.fetch(raw, AppInfo.VERSION).onSuccess { d ->
                val e = existing ?: SubEntry((subs.maxOfOrNull { it.id } ?: 0) + 1, d).also { subs.add(it); if (subs.size == 1) activeId = it.id }
                e.data = d; e.pings.clear()
                if (e.selected >= d.servers.size) e.selected = 0
                saveSubs()
                AppLog.add("SUB", (if (existing == null) "Подписка добавлена" else "Подписка обновлена") + ": серверов ${d.servers.size}")
                if (!silent) toast = if (existing == null) tr("Подписка добавлена") else tr("Подписка обновлена")
                checkReminders()
                if (state == ConnState.Connected && existing != null && existing.id == active?.id && !silent && raw != existing.data.url) reconnect()
            }.onFailure {
                AppLog.add("SUB", "Ошибка: ${it.message}" + if (it.message?.startsWith("Это не похоже") == true) " (получено: ${LinkNormalizer.head(raw)})" else "")
                if (!silent) error = (if (existing == null) tr("Не получилось") else tr("Не удалось обновить")) to (it.message ?: tr("Ошибка"))
            }
            existing?.refreshing = false
        }
    }

    fun deleteSubscription(e: SubEntry) {
        val was = isActive(e)
        if (was) disconnect()
        subs.remove(e)
        if (was) activeId = subs.firstOrNull()?.id ?: -1
        saveSubs()
    }

    fun togglePin(e: SubEntry) { e.pinned = !e.pinned; saveSubs() }

    /** Выбор сервера в любой карточке: подписка карточки становится активной. */
    fun select(e: SubEntry, i: Int) {
        if (isActive(e) && i == e.selected) return
        activeId = e.id; e.selected = i; saveSubs()
        if (state != ConnState.Idle) reconnect()
    }

    fun toggle() {
        when (state) {
            ConnState.Idle -> if (sub == null) error = tr("Сначала подписка") to tr("Добавьте ссылку подписки, чтобы подключиться.") else connect()
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
                error = tr("Подключение прервано") to (tail ?: tr("Ядро остановилось"))
            }.onSuccess {
                if (mode == VpnMode.Proxy) runCatching { proxy.enable(CoreProcess.HTTP_PORT) }.onFailure { error = tr("Системный прокси") to (it.message ?: tr("Не удалось включить")) }
                connectedAt = System.currentTimeMillis(); state = ConnState.Connected; AppLog.add("VPN", "Подключено"); watch.start()
            }.onFailure { state = ConnState.Idle; AppLog.add("VPN", "Не подключилось: ${it.message}"); error = tr("Не подключилось") to (it.message ?: tr("Ошибка ядра")) }
        }
    }

    /** TUN требует администратора: перезапускаемся с правами (окно UAC) и подключаемся уже там. */
    private fun elevateAndConnect() {
        if (Elevation.relaunch(listOf("--connect"))) { shutdown(); exitRequested = true }
        else error = tr("Нужны права администратора") to tr("Режим «Все программы» создаёт виртуальную сетевую карту — подтвердите запрос Windows. Или выберите «Системный прокси»: он работает без администратора.")
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

    fun pingAll(e: SubEntry) {
        if (e.pinging) return
        e.pinging = true; e.pings.clear()
        scope.launch {
            PingRunner.measure(e.data.body, e.data.servers.indices.toList()) { i, ms -> e.pings[i] = ms }
            e.pinging = false
        }
    }

    /** «Выбрать быстрейший»: пингуем все серверы подписки и выбираем лучший по результату. */
    fun selectFastest(e: SubEntry) {
        if (e.pinging) return
        toast = tr("Измеряем пинг…"); e.pinging = true; e.pings.clear()
        scope.launch {
            PingRunner.measure(e.data.body, e.data.servers.indices.toList()) { i, ms -> e.pings[i] = ms }
            e.pinging = false
            val best = e.pings.filter { it.value > 0 }.minByOrNull { it.value }
            if (best == null) toast = tr("Ни один сервер не ответил")
            else { toast = trf("Быстрейший: %s · %d мс", e.data.servers[best.key].title, best.value); select(e, best.key) }
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
        val act = active ?: return
        PingRunner.measure(s.body, s.servers.indices.toList()) { i, ms -> res[i] = ms; act.pings[i] = ms }
        val best = res.filter { it.key != cur && it.value > 0 && s.servers.getOrNull(it.key)?.let { sv -> switchFilter.allowed(s.servers, sv) } == true }.minByOrNull { it.value }?.key
        val now = System.currentTimeMillis()
        while (switchTimes.isNotEmpty() && now - switchTimes.first() > 600_000L) switchTimes.removeFirst()
        if (best != null && (res[cur] ?: -1) <= 0 && switchTimes.size < 3) {
            switchTimes.addLast(now)
            val msg = trf("«%s» не отвечал — подключились к «%s»", s.servers.getOrNull(cur)?.title, s.servers[best].title)
            selected = best
            AppLog.add("VPN", "Сервер сменился: $msg"); toast = msg; notifier?.invoke(tr("Сервер сменился"), msg, null)
        } else AppLog.add("VPN", if (res.values.none { it > 0 }) "Ни один сервер не отвечает — похоже, нет интернета, переподключаемся" else "Сервер отвечает, переподключаемся")
        connect()
    }

    /** Пинг одного сервера активной подписки (кнопка справа). */
    fun pingServer(i: Int) { active?.let { pingServer(it, i) } }

    /** Пинг одного сервера любой подписки (правая кнопка по серверу). */
    fun pingServer(e: SubEntry, i: Int) {
        if (i in e.pingBusy) return
        e.pingBusy.add(i); e.pings.remove(i)
        scope.launch { PingRunner.measure(e.data.body, listOf(i)) { k, ms -> e.pings[k] = ms }; e.pingBusy.remove(i) }
    }

    fun setTheme(m: Int) { themeMode = m; settings.set("theme", m) }
    fun choosePalette(p: Int) { palette = p; settings.set("palette", p) }
    fun chooseAmoled(v: Boolean) { amoled = v; settings.set("amoled", v) }
    fun chooseLang(v: Int) = Lang.set(settings, v)
    fun chooseBlockAds(v: Boolean) { blockAds = v; settings.set("blockAds", v); if (state != ConnState.Idle) reconnect() }
    fun chooseAllVpn(v: Boolean) { allVpn = v; settings.set("allVpn", v); if (state != ConnState.Idle) reconnect() }
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