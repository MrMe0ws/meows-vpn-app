package app.meows.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
import app.meows.desktop.data.SettingsStore
import app.meows.desktop.system.Elevation
import app.meows.desktop.system.Protocol
import app.meows.desktop.system.SingleInstance
import app.meows.desktop.ui.MeowsApp
import app.meows.desktop.ui.Res
import app.meows.desktop.ui.theme.MeowsTheme
import java.util.concurrent.TimeUnit

/** Сообщения от второго запуска могут прийти раньше, чем создана модель, — складываем в очередь. */
private object Inbox {
    @Volatile private var model: AppModel? = null
    private val queue = ArrayDeque<String>()
    @Synchronized fun deliver(msg: String) { model?.handleExternal(msg) ?: queue.addLast(msg) }
    @Synchronized fun attach(m: AppModel) { model = m; while (queue.isNotEmpty()) m.handleExternal(queue.removeFirst()) }
}

fun main(args: Array<String>) {
    val link = args.firstOrNull { it.startsWith("meows://") || it.startsWith("happ://") }
    // нас перезапустили с правами администратора — ждём, пока закроется прежний экземпляр (он держит «единственный экземпляр»)
    args.indexOf("--wait-pid").takeIf { it >= 0 }?.let { args.getOrNull(it + 1)?.toLongOrNull() }?.let { pid ->
        ProcessHandle.of(pid).ifPresent { h -> runCatching { h.onExit().get(15, TimeUnit.SECONDS) } }
    }
    // уже запущены — передаём ссылку/«показать окно» первому экземпляру и выходим
    if (!SingleInstance.start { Inbox.deliver(it) }) { SingleInstance.sendToRunning(link ?: "SHOW"); return }
    Protocol.register()
    // режим «Все программы» (по умолчанию) требует администратора: просим права сразу при запуске, чтобы не переключаться потом
    if (!Elevation.isAdmin() && SettingsStore().str("mode", "tun") == "tun" && "--no-elevate" !in args) {
        val extra = listOfNotNull(link, "--minimized".takeIf { it in args }, "--connect".takeIf { it in args })
        if (Elevation.relaunch(extra)) return
    }

    application {
        val app = remember { AppModel(startHidden = "--minimized" in args && link == null, connectOnStart = "--connect" in args).also { Inbox.attach(it); link?.let(Inbox::deliver) } }
        val trayOk = isTraySupported
        val icon = BitmapPainter(Res.bitmap("icon.png"))
        LaunchedEffect(app.exitRequested) { if (app.exitRequested) exitApplication() }

        if (trayOk) Tray(
            icon = icon, tooltip = "Meows",
            onAction = { app.windowVisible = true; app.showTick++ },
            menu = {
                Item("Открыть Meows", onClick = { app.windowVisible = true; app.showTick++ })
                Item(if (app.state == ConnState.Idle) "Подключить" else "Отключить", onClick = { app.toggle() })
                Separator()
                Item("Выход", onClick = { app.shutdown(); exitApplication() })
            },
        )

        Window(
            visible = app.windowVisible,
            onCloseRequest = { if (trayOk && app.closeToTray) app.windowVisible = false else { app.shutdown(); exitApplication() } },
            title = "Meows", icon = icon,
            state = rememberWindowState(size = DpSize(1100.dp, 720.dp)),
        ) {
            window.minimumSize = java.awt.Dimension(900, 600)
            LaunchedEffect(app.showTick) { if (app.showTick > 0) { window.toFront(); window.requestFocus() } }
            MeowsTheme(app.themeMode, app.palette, app.amoled) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) { MeowsApp(app) }
            }
        }
    }
}