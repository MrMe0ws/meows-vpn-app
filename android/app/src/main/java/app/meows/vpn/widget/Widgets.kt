package app.meows.vpn.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import app.meows.vpn.MainActivity
import app.meows.vpn.XKStore
import app.meows.vpn.XKVpnService

/** Общая логика виджетов подключения: касание, выбранный сервер, обновление по состоянию VPN. */
object Widgets {
    const val ACTION_TOGGLE = "app.meows.vpn.widget.TOGGLE"
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /** Сервер, на который подключится (или уже подключён) VPN: последний выбранный в активной подписке. */
    class Server(val flag: String, val title: String)

    fun server(c: Context): Server? = try {
        val st = XKStore.get(c)
        val list = if (st.hasSubscription()) st.servers() else emptyList()
        val s = list.getOrNull(st.index())
        s?.let { Server(it.flag?.takeIf { f -> f.isNotBlank() } ?: "🌐", it.title ?: it.name ?: "") }
    } catch (e: Exception) { null }

    /** Нет разрешения VPN или подписки — касание должно открыть приложение. */
    private fun needsApp(c: Context) = VpnService.prepare(c) != null || !XKStore.get(c).hasSubscription()

    private fun openApp(c: Context) = Intent(c, MainActivity::class.java)

    /** Реакция на касание: выключить, включить или открыть приложение. */
    fun toggle(c: Context) {
        if (XKVpnService.state != XKVpnService.IDLE) XKVpnService.stop(c)
        else if (needsApp(c)) c.startActivity(openApp(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        else XKVpnService.start(c)
        refresh(c)
    }

    /** Открыть приложение. */
    fun openIntent(c: Context): PendingIntent = PendingIntent.getActivity(c, 1, openApp(c), FLAGS)

    /** Включить/выключить (или открыть приложение, если без него не обойтись). */
    fun toggleIntent(c: Context, provider: Class<*>): PendingIntent =
        if (XKVpnService.state == XKVpnService.IDLE && needsApp(c)) PendingIntent.getActivity(c, 0, openApp(c), FLAGS)
        else PendingIntent.getBroadcast(c, 0, Intent(c, provider).setAction(ACTION_TOGGLE), FLAGS)

    fun ids(c: Context, provider: Class<*>): IntArray =
        AppWidgetManager.getInstance(c).getAppWidgetIds(ComponentName(c, provider))

    /** Перерисовать все виджеты (зовётся из сервиса при смене состояния VPN). */
    @JvmStatic
    fun refresh(context: Context?) {
        val c = context?.applicationContext ?: return
        val m = AppWidgetManager.getInstance(c)
        val big = ids(c, ConnectWidgetBig::class.java)
        val small = ids(c, ConnectWidgetSmall::class.java)
        if (big.isEmpty() && small.isEmpty()) return
        val level = WidgetAnimator.level
        if (big.isNotEmpty()) m.updateAppWidget(big, ConnectWidgetBig.build(c, level))
        if (small.isNotEmpty()) m.updateAppWidget(small, ConnectWidgetSmall.build(c, level))
        WidgetAnimator.kick(c)
    }
}
