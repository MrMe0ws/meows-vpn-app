package app.meows.desktop.log

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date

data class LogLine(val time: Long, val tag: String, val text: String)

/** Журнал событий приложения в памяти (последние ~1500 строк): подключение, подписка, ядро. Ссылки подписки сюда не пишем. */
object AppLog {
    val lines = mutableStateListOf<LogLine>()
    private val fmt = SimpleDateFormat("HH:mm:ss")

    @Synchronized fun add(tag: String, text: String) {
        lines.add(LogLine(System.currentTimeMillis(), tag, text))
        if (lines.size > 1500) lines.subList(0, 300).clear()
    }

    @Synchronized fun clear() = lines.clear()

    fun format(l: LogLine) = "[${fmt.format(Date(l.time))}] [${l.tag}] ${l.text}"
}
