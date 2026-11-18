package app.meows.vpn.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import app.meows.vpn.R
import app.meows.vpn.XKVpnService

/**
 * Виджет 1×1: круг с лапкой. Выключен — приглушённый диск, включён — яркий диск с ореолом
 * и маленький флаг сервера справа внизу. Плавность — см. [WidgetAnimator].
 */
class ConnectWidgetSmall : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = Widgets.refresh(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Widgets.ACTION_TOGGLE) Widgets.toggle(context)
    }

    companion object {
        fun build(c: Context, level: Int): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.widget_connect_small)
            WidgetAnimator.applyLevel(v, level)
            val server = if (XKVpnService.state == XKVpnService.CONNECTED) Widgets.server(c) else null
            v.setViewVisibility(R.id.w_flag, if (server != null) View.VISIBLE else View.GONE)
            if (server != null) v.setTextViewText(R.id.w_flag, server.flag)
            v.setOnClickPendingIntent(R.id.w_root, Widgets.toggleIntent(c, ConnectWidgetSmall::class.java))
            return v
        }
    }
}
