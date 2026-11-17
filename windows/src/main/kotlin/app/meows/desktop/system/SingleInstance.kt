package app.meows.desktop.system

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Один экземпляр приложения. Первый запуск занимает локальный порт и слушает; повторный запуск (ярлык, ссылка meows://…)
 * отправляет первому сообщение («SHOW» или ссылку) и завершается.
 */
object SingleInstance {
    private const val PORT = 47831
    private val loopback = InetAddress.getByName("127.0.0.1")
    private var server: ServerSocket? = null

    /** true — мы первый экземпляр. [onMessage] получает строки от следующих запусков. */
    fun start(onMessage: (String) -> Unit): Boolean {
        val s = try { ServerSocket(PORT, 8, loopback) } catch (e: Exception) { return false }
        server = s
        thread(isDaemon = true, name = "meows-single-instance") {
            while (!s.isClosed) {
                runCatching {
                    s.accept().use { c -> c.getInputStream().bufferedReader().readLine()?.let(onMessage) }
                }
            }
        }
        return true
    }

    fun sendToRunning(message: String) {
        runCatching { Socket(loopback, PORT).use { it.getOutputStream().apply { write((message + "\n").toByteArray()); flush() } } }
    }
}