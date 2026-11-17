package app.meows.desktop.vpn

import app.meows.desktop.data.AppDirs
import app.meows.desktop.data.SubscriptionData
import app.meows.desktop.log.AppLog
import app.meows.desktop.stats.StatsFeed
import org.json.JSONObject
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Ядро Xray (meows-core.exe) как дочерний процесс: режим proxy — локальные SOCKS5/HTTP на 127.0.0.1, режим tun — адаптер wintun (процесс наследует права администратора). */
class CoreProcess {
    companion object { const val SOCKS_PORT = 10808; const val HTTP_PORT = 10809 }

    @Volatile private var proc: Process? = null
    @Volatile private var stopping = false

    val running: Boolean get() = proc?.isAlive == true

    /** Запускает ядро и ждёт `XKOK` (до 20 с). [onExit] вызывается, если ядро завершилось не по нашей просьбе. */
    fun start(sub: SubscriptionData, index: Int, opts: String, mode: String, onExit: (String?) -> Unit): Result<Unit> = runCatching {
        stop()
        require(AppDirs.coreExe.exists()) { "Не найден meows-core.exe (${AppDirs.coreExe})" }
        syncGeo()
        val cfg = JSONObject().put("sub", sub.body).put("index", index).put("opts", opts)
            .put("socksPort", SOCKS_PORT).put("httpPort", HTTP_PORT).put("mode", mode).put("traffic", true)
        val f = AppDirs.file("core.json").apply { writeText(cfg.toString()) }
        stopping = false
        val p = ProcessBuilder(AppDirs.coreExe.absolutePath, f.absolutePath, AppDirs.geo.absolutePath).redirectErrorStream(true).start()
        proc = p
        val lines = LinkedBlockingQueue<String>()
        thread(isDaemon = true, name = "meows-core-out") {
            p.inputStream.bufferedReader().useLines { s -> s.forEach { if (!StatsFeed.accept(it) && !AppLog.addCoreLine(it)) lines.put(it) } }
            lines.put("\u0000EXIT")
        }
        val err = StringBuilder()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        var ok = false
        while (System.nanoTime() < deadline) {
            val l = lines.poll(200, TimeUnit.MILLISECONDS) ?: continue
            if (l.startsWith("XKOK")) { ok = true; break }
            if (l.startsWith("XKERR")) err.append(l.removePrefix("XKERR").trim())
            if (l == "\u0000EXIT") break
        }
        if (!ok) { stop(); error(err.toString().ifBlank { "Ядро не запустилось" }) }
        thread(isDaemon = true, name = "meows-core-watch") {
            val tail = StringBuilder()
            while (true) {
                val l = lines.take()
                if (l == "\u0000EXIT") break
                if (tail.length < 300) tail.append(l).append(' ')
            }
            if (!stopping) onExit(tail.toString().trim().ifBlank { null })
        }
    }

    /** Закрываем stdin — ядро завершается само; если нет — убиваем. */
    fun stop() {
        val p = proc ?: return
        stopping = true
        runCatching { p.outputStream.close() }
        if (!p.waitFor(2, TimeUnit.SECONDS)) p.destroyForcibly()
        proc = null
    }

    /** Гео-файлы из ресурсов приложения → %APPDATA%\Meows\geo (если не скачаны свежие по ссылкам панели). */
    private fun syncGeo() {
        if (File(AppDirs.geo, "custom").exists()) return
        for (n in listOf("geoip.dat", "geosite.dat")) {
            val src = File(AppDirs.resources, n)
            val dst = File(AppDirs.geo, n)
            if (src.exists() && (!dst.exists() || dst.length() != src.length())) src.copyTo(dst, overwrite = true)
        }
    }
}