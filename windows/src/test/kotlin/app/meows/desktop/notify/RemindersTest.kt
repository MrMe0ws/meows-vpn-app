package app.meows.desktop.notify

import app.meows.desktop.data.SubscriptionData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemindersTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    private fun sub(expireMs: Long, used: Long = 0, total: Long = 0): SubscriptionData {
        val info = "upload=0; download=$used; total=$total; expire=${expireMs / 1000}"
        return SubscriptionData("https://x/y", "[]", mapOf("subscription-userinfo" to info), 0)
    }

    private fun keys(s: SubscriptionData) = Reminders.due(s, now).map { it.key.substringAfterLast(':') + it.kind }

    @Test fun farFutureNothing() = assertTrue(Reminders.due(sub(now + 30 * day), now).isEmpty())
    @Test fun threeDays() = assertEquals(listOf("3exp"), keys(sub(now + 3 * day - 1000)))
    @Test fun oneDay() = assertEquals(listOf("1exp"), keys(sub(now + day - 1000)))
    @Test fun expired() = assertEquals(listOf("0exp"), keys(sub(now - 1000)))
    @Test fun noExpiryNoReminder() = assertTrue(Reminders.due(sub(0), now).isEmpty())
    @Test fun traffic90() = assertEquals(listOf("90trf"), keys(sub(now + 30 * day, used = 91, total = 100)))
    @Test fun traffic100() = assertEquals(listOf("100trf"), keys(sub(now + 30 * day, used = 120, total = 100)))
    @Test fun trafficBelowThreshold() = assertTrue(Reminders.due(sub(now + 30 * day, used = 50, total = 100), now).isEmpty())
    @Test fun unlimitedTraffic() = assertTrue(Reminders.due(sub(now + 30 * day, used = 999, total = 0), now).isEmpty())
    @Test fun bothAtOnce() = assertEquals(listOf("1exp", "90trf"), keys(sub(now + day - 1000, used = 95, total = 100)))
    @Test fun keyChangesWhenRenewed() {
        val a = Reminders.due(sub(now + 2 * day), now).first().key
        val b = Reminders.due(sub(now + 2 * day + 30 * day), now)
        assertTrue(b.isEmpty() || b.first().key != a)
    }
}
