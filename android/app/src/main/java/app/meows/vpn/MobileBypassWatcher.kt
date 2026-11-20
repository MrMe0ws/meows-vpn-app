package app.meows.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import app.meows.vpn.i18n.Lang
import java.util.concurrent.Executors

/**
 * Следит, пока VPN подключён, за сменой сети Wi-Fi ↔ мобильная и просит [MobileBypass] сменить сервер.
 * Смена сети учитывается через [DEBOUNCE_MS] (Wi-Fi любит мигать), переключения — не чаще раза в [MIN_GAP_MS].
 * Если ни один сервер обхода не ответил, пробуем ещё [RETRIES] раза с паузой.
 */
class MobileBypassWatcher(private val ctx: Context, private val restart: Runnable) {
    private val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var callback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var stopped = false
    private var lastTransport = MobileBypass.NONE
    private var lastApplied = 0L
    @Volatile private var attempt = 0
    private val checkTask = Runnable { check(false) }

    fun start() {
        lastTransport = MobileBypass.transport(ctx)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = poke()
            override fun onLost(network: Network) = poke()
        }
        callback = cb
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build()
        runCatching { cm.registerNetworkCallback(req, cb) }
        current = this
    }

    fun stop() {
        stopped = true
        if (current === this) current = null
        handler.removeCallbacksAndMessages(null)
        callback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        callback = null
        worker.shutdown()   // не прерываем: stop() может вызваться из самого рабочего потока (перезапуск туннеля)
    }

    private fun poke() { handler.removeCallbacks(checkTask); handler.postDelayed(checkTask, DEBOUNCE_MS) }

    /** [force] — проверить даже без смены сети (функцию только что включили или повторная попытка). */
    private fun check(force: Boolean) {
        if (stopped) return
        val t = MobileBypass.transport(ctx)
        if (t != lastTransport) { lastTransport = t; attempt = 0 } else if (!force) return
        val wait = lastApplied + MIN_GAP_MS - System.currentTimeMillis()
        if (wait > 0) { handler.removeCallbacks(checkTask); handler.postDelayed({ check(true) }, wait); return }
        runCatching { worker.execute { run(t) } }
    }

    private fun run(t: Int) {
        val change = runCatching { MobileBypass.evaluate(ctx) }.getOrNull()
        if (stopped) return
        if (change != null) {
            lastApplied = System.currentTimeMillis()
            notifyChange(change)
            restart.run()
        } else if (t == MobileBypass.CELL && ++attempt <= RETRIES && MobileBypass.pending(ctx)) {
            handler.postDelayed({ check(true) }, RETRY_MS)
        }
    }

    private fun notifyChange(c: MobileBypass.Change) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel("switch") == null) {
            nm.createNotificationChannel(NotificationChannel("switch", Lang.tr("Смена сервера"), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val fl = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, "switch") else Notification.Builder(ctx)
        val text = (if (c.back) Lang.tr("Wi-Fi: вернулись на «") else Lang.tr("Мобильная сеть: подключились к «")) + c.to + "»"
        b.setSmallIcon(R.drawable.ic_stat).setContentTitle(Lang.tr("Сервер сменился")).setContentText(text)
            .setContentIntent(PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), fl)).setAutoCancel(true).setColor(0xFF7CD62B.toInt())
        nm.notify(31, b.build())
    }

    companion object {
        private const val DEBOUNCE_MS = 4_000L
        private const val MIN_GAP_MS = 30_000L
        private const val RETRY_MS = 20_000L
        private const val RETRIES = 3
        @Volatile private var current: MobileBypassWatcher? = null

        /** Проверить сеть сразу (например, функцию только что включили в настройках). */
        @JvmStatic fun recheck() { current?.let { w -> w.handler.post { w.check(true) } } }
    }
}
