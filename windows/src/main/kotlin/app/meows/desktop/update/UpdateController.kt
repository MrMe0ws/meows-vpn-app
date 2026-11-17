package app.meows.desktop.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.meows.desktop.AppInfo
import app.meows.desktop.data.SettingsStore
import app.meows.desktop.system.ExePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Обновление приложения: проверка GitHub (при запуске не чаще раза в 12 ч, если включено), загрузка .msi, тихая установка и перезапуск. */
class UpdateController(private val settings: SettingsStore, private val scope: CoroutineScope) {
    var state: UpdateState by mutableStateOf(UpdateState.Idle); private set
    var dialogVisible by mutableStateOf(false); private set
    private var pending: ReleaseInfo? = null

    var autoCheck: Boolean
        get() = settings.bool("updateCheck", true)
        set(v) = settings.set("updateCheck", v)

    fun checkOnLaunch() {
        if (!autoCheck || System.currentTimeMillis() - settings.long("updateLastCheck") < 12 * 3_600_000L) return
        check(manual = false)
    }

    fun check(manual: Boolean) {
        if (state is UpdateState.Checking || state is UpdateState.Downloading) return
        state = UpdateState.Checking
        scope.launch {
            val r = UpdateChecker.latest()
            settings.set("updateLastCheck", System.currentTimeMillis())
            state = when (r) {
                is UpdateChecker.Result.Found -> if (Version.isNewer(r.info.version, AppInfo.VERSION)) UpdateState.Available(r.info) else UpdateState.UpToDate
                UpdateChecker.Result.NoReleases -> UpdateState.UpToDate
                is UpdateChecker.Result.Failed -> if (manual) UpdateState.Error(r.message) else UpdateState.Idle
            }
            (state as? UpdateState.Available)?.let { pending = it.info; dialogVisible = true }
        }
    }

    fun download() {
        val info = pending ?: return
        if (state is UpdateState.Downloading) return
        state = UpdateState.Downloading(info, 0)
        scope.launch {
            state = runCatching {
                val dir = File(System.getProperty("java.io.tmpdir"), "MeowsUpdate").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                val out = File(dir, "Meows-${info.version}.msi")
                val c = URL(info.msiUrl).openConnection() as HttpURLConnection
                c.connectTimeout = 15_000; c.readTimeout = 30_000; c.setRequestProperty("User-Agent", "Meows-Windows")
                val total = c.contentLengthLong.takeIf { it > 0 } ?: info.size
                var done = 0L; var last = -1
                c.inputStream.use { i -> out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024); var n: Int
                    while (i.read(buf).also { n = it } > 0) {
                        o.write(buf, 0, n); done += n
                        if (total > 0) { val p = (done * 100 / total).toInt(); if (p != last) { last = p; state = UpdateState.Downloading(info, p) } }
                    }
                } }
                c.disconnect()
                if (total > 0 && done != total) { out.delete(); error("Файл скачан не полностью") }
                UpdateState.Downloaded(info, out.absolutePath)
            }.getOrElse { UpdateState.Error("Не удалось скачать: ${it.message}") }
        }
    }

    /**
     * Запускает установку в отдельном процессе (после выхода приложения) и запускает приложение снова. Вызывающий должен закрыть приложение.
     * Команды лежат во временном .cmd: `start /wait` дожидается msiexec (он GUI-процесс, `cmd` сам его не ждёт — раньше приложение
     * «перезапускалось» до конца установки и не поднималось), а кавычки в путях не ломаются. Папка установки сохраняется (INSTALLDIR),
     * чтобы обновление не переезжало в папку по умолчанию, если пользователь выбрал свою.
     */
    fun launchInstaller(): Boolean {
        val s = state as? UpdateState.Downloaded ?: return false
        val exe = ExePath.current()
        val dir = exe?.let { File(it).parent }
        val script = File.createTempFile("meows-update", ".cmd")
        script.writeText(
            buildString {
                appendLine("@echo off")
                appendLine("chcp 65001 >nul")
                appendLine("ping -n 3 127.0.0.1 >nul")
                appendLine("start /wait \"\" msiexec /i \"${s.path}\" /passive /norestart" + if (dir != null) " INSTALLDIR=\"$dir\"" else "")
                if (exe != null) appendLine("start \"\" \"$exe\"")
                appendLine("(goto) 2>nul & del \"%~f0\"")
            },
            Charsets.UTF_8,
        )
        ProcessBuilder("cmd", "/c", script.absolutePath).start()
        return true
    }

    fun dismissDialog() { dialogVisible = false }
}