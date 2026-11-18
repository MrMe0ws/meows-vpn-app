package app.meows.vpn.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import app.meows.vpn.R
import app.meows.vpn.XKVpnService
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * «Свечение» кружка в виджетах. Виджеты не умеют настоящих анимаций, поэтому плавность — частые лёгкие обновления:
 * [level] (0..255) — прозрачность «горящего» слоя поверх приглушённого диска.
 * Выключен — 0, подключён — 255, при подключении — медленное «дыхание». Между значениями уровень идёт ограниченными шагами,
 * поэтому включение и выключение тоже плавные.
 */
object WidgetAnimator {
    private const val TICK_MS = 90L
    private const val STEP = 40            // максимум за тик (≈0,6 с от 0 до 255)
    private const val BREATH_MS = 2600.0   // период «дыхания»
    private const val BREATH_MIN = 50
    private const val BREATH_MAX = 255

    @Volatile var level = 0
        private set

    private val main = Handler(Looper.getMainLooper())
    private var running = false
    private var breathStart = 0L

    /** Прозрачность «горящих» слоёв (подсветка диска и лапка) в разметке виджета. */
    fun applyLevel(v: RemoteViews, level: Int) {
        v.setInt(R.id.w_lit, "setImageAlpha", level)
        v.setInt(R.id.w_paw_on, "setImageAlpha", level)
    }

    private fun goal(now: Long): Int = when (XKVpnService.state) {
        XKVpnService.CONNECTED -> 255
        XKVpnService.CONNECTING -> {
            if (breathStart == 0L) breathStart = now
            val mid = (BREATH_MAX + BREATH_MIN) / 2.0
            val amp = (BREATH_MAX - BREATH_MIN) / 2.0
            (mid - amp * cos(2 * PI * (now - breathStart) / BREATH_MS)).roundToInt()
        }
        else -> { breathStart = 0L; 0 }
    }

    /** Запустить покадровую анимацию, если уровень ещё не дошёл до цели. */
    fun kick(c: Context) {
        if (running) return
        running = true
        main.post(object : Runnable {
            override fun run() {
                val m = AppWidgetManager.getInstance(c)
                val big = Widgets.ids(c, ConnectWidgetBig::class.java)
                val small = Widgets.ids(c, ConnectWidgetSmall::class.java)
                val g = goal(System.currentTimeMillis())
                level += (g - level).coerceIn(-STEP, STEP)
                if (big.isNotEmpty()) m.partiallyUpdateAppWidget(big, RemoteViews(c.packageName, R.layout.widget_connect_big).also { applyLevel(it, level) })
                if (small.isNotEmpty()) m.partiallyUpdateAppWidget(small, RemoteViews(c.packageName, R.layout.widget_connect_small).also { applyLevel(it, level) })
                val done = XKVpnService.state != XKVpnService.CONNECTING && level == g
                if (done || (big.isEmpty() && small.isEmpty())) running = false else main.postDelayed(this, TICK_MS)
            }
        })
    }
}
