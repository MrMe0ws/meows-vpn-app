package app.meows.desktop.data

import java.text.SimpleDateFormat
import java.util.Date

/**
 * Файл `%APPDATA%\Meows\launch.log`: с какими аргументами Windows запускала приложение и что получил первый экземпляр.
 * Нужен, чтобы разбираться со ссылками `meows://` из браузера. Пишется как пришло (там может быть токен подписки) —
 * файл только локальный, в интерфейсе и в «Скопировать» логов его нет. Хранится до ~100 КБ.
 */
object LaunchLog {
    private val file get() = AppDirs.file("launch.log")
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    @Synchronized fun write(line: String) {
        runCatching {
            if (file.length() > 100_000) file.writeText("")
            file.appendText("${fmt.format(Date())} $line\n")
        }
    }
}
