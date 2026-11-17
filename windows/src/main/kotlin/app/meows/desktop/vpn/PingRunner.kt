package app.meows.desktop.vpn

import app.meows.desktop.data.AppDirs

/** Реальный пинг: ядро строит исходящее подключение к каждому серверу и запрашивает тестовый адрес через него (как в Android-версии). */
object PingRunner {
    const val TEST_URL = "https://www.gstatic.com/generate_204"

    /** Блокирующий вызов. [onResult] получает (индекс сервера, мс; -1 — нет ответа). */
    fun measure(body: String, indexes: List<Int>, timeoutMs: Int = 3000, onResult: (Int, Int) -> Unit) {
        if (indexes.isEmpty() || !AppDirs.coreExe.exists()) return
        val f = AppDirs.file("measure.json").apply { writeText(body) }
        val got = HashSet<Int>()
        runCatching {
            val p = ProcessBuilder(
                AppDirs.coreExe.absolutePath, "measure", f.absolutePath, TEST_URL, timeoutMs.toString(), indexes.joinToString(","),
            ).redirectErrorStream(true).start()
            val limit = System.currentTimeMillis() + (timeoutMs * 2L + 2500) * ((indexes.size + 3) / 4) + 3000
            val killer = Thread { while (p.isAlive && System.currentTimeMillis() < limit) Thread.sleep(200); if (p.isAlive) p.destroyForcibly() }
            killer.isDaemon = true; killer.start()
            p.inputStream.bufferedReader().useLines { lines ->
                for (l in lines) {
                    if (!l.startsWith("XKMS ")) continue
                    val a = l.trim().split(" ")
                    if (a.size < 3) continue
                    val i = a[1].toIntOrNull() ?: continue
                    val ms = a[2].toIntOrNull() ?: continue
                    got.add(i); onResult(i, if (ms > 0) ms else -1)
                }
            }
            p.waitFor()
        }
        for (i in indexes) if (i !in got) onResult(i, -1)
    }
}