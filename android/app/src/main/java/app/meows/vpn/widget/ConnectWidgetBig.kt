package app.meows.vpn.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.meows.vpn.R
import app.meows.vpn.XKVpnService

/**
 * Виджет 2×2: шапка (лапка, «Meows», состояние), плашка с выбранным сервером и кнопка «Включить» / «Отключить».
 * Кнопка и кружок включают/выключают VPN, остальная площадь открывает приложение.
 * Цвета — системные динамические (Material You), см. widget_colors.xml в папках res (values, -night, -v31).
 */
class ConnectWidgetBig : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = Widgets.refresh(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Widgets.ACTION_TOGGLE) Widgets.toggle(context)
    }

    companion object {
        fun build(c: Context, level: Int): RemoteViews {
            val state = XKVpnService.state
            val on = state != XKVpnService.IDLE
            val v = RemoteViews(c.packageName, R.layout.widget_connect_big)
            WidgetAnimator.applyLevel(v, level)

            v.setInt(R.id.w_root, "setBackgroundResource", if (on) R.drawable.widget_bg_on else R.drawable.widget_bg_off)
            v.setInt(R.id.w_pill, "setBackgroundResource", if (on) R.drawable.widget_pill_on else R.drawable.widget_pill_off)
            val fg = c.getColor(if (on) R.color.w_fg_on else R.color.w_fg_off)
            for (id in intArrayOf(R.id.w_title, R.id.w_status, R.id.w_server)) v.setTextColor(id, fg)
            v.setTextViewText(
                R.id.w_status,
                c.getString(
                    when (state) {
                        XKVpnService.CONNECTED -> R.string.widget_on
                        XKVpnService.CONNECTING -> R.string.widget_connecting
                        else -> R.string.widget_off
                    }
                )
            )

            val server = Widgets.server(c)
            v.setTextViewText(R.id.w_flag, server?.flag ?: "")
            v.setTextViewText(R.id.w_server, server?.title ?: c.getString(R.string.widget_no_server))

            // выключен — заливная кнопка «Включить», включён — контурная «Отключить»
            v.setTextViewText(R.id.w_button, c.getString(if (on) R.string.widget_disconnect else R.string.widget_connect))
            v.setInt(R.id.w_button, "setBackgroundResource", if (on) R.drawable.widget_btn_outlined else R.drawable.widget_btn_filled)
            v.setTextColor(R.id.w_button, c.getColor(if (on) R.color.w_fg_on else R.color.w_icon_on))

            val toggle = Widgets.toggleIntent(c, ConnectWidgetBig::class.java)
            v.setOnClickPendingIntent(R.id.w_root, Widgets.openIntent(c))
            v.setOnClickPendingIntent(R.id.w_disc, toggle)
            v.setOnClickPendingIntent(R.id.w_button, toggle)
            return v
        }
    }
}
