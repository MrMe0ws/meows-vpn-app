package app.meows.desktop.notify

import app.meows.desktop.data.SubscriptionData

/** Напоминания о подписке и трафике (те же уровни и тексты, что на Android): 3 дня, 1 день, закончилась; трафик 90 % и 100 %. */
object Reminders {
    /** [key] уникален для «вид + срок подписки + уровень» — по нему не повторяем уведомление. */
    data class Note(val kind: String, val key: String, val title: String, val text: String)

    fun due(sub: SubscriptionData, now: Long = System.currentTimeMillis()): List<Note> {
        val out = ArrayList<Note>()
        val exp = sub.expireSec
        if (exp > 0) {
            val left = exp * 1000 - now
            val day = 86_400_000L
            when {
                left <= 0 -> out += Note("exp", "exp:$exp:0", "Подписка закончилась", "Продлите её в личном кабинете и нажмите «Обновить».")
                left <= day -> out += Note("exp", "exp:$exp:1", "Завтра заканчивается подписка", "Остался 1 день. Продлите подписку, чтобы VPN не отключился.")
                left <= 3 * day -> out += Note("exp", "exp:$exp:3", "Подписка скоро закончится", "Через 3 дня подписка закончится. Нажмите, чтобы продлить в личном кабинете.")
            }
        }
        if (sub.total > 0) {
            val pct = (sub.used * 100 / sub.total).toInt()
            when {
                pct >= 100 -> out += Note("trf", "trf:$exp:100", "Трафик закончился", "Лимит исчерпан. Продлите или докупите трафик в личном кабинете.")
                pct >= 90 -> out += Note("trf", "trf:$exp:90", "Трафик почти закончился", "Использовано $pct% трафика. Нажмите, чтобы открыть личный кабинет.")
            }
        }
        return out
    }
}
