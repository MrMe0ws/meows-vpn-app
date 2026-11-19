package app.meows.vpn

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Варианты иконки приложения: на каждый — activity-alias в манифесте; включён ровно один. */
enum class AppIcon(val alias: String, val title: String, val preview: Int) {
    Default("LauncherDefault", "Зелёная лапа", R.drawable.icon_prev_default),
    Glow("LauncherGlow", "Свечение", R.drawable.icon_prev_glow),
    Lime("LauncherLime", "Лайм", R.drawable.icon_prev_lime),
    Classic("LauncherClassic", "Классика", R.drawable.icon_prev_classic);

    private fun component(ctx: Context) = ComponentName(ctx.packageName, "app.meows.vpn.$alias")

    companion object {
        fun current(ctx: Context): AppIcon {
            val pm = ctx.packageManager
            return entries.firstOrNull { icon ->
                when (pm.getComponentEnabledSetting(icon.component(ctx))) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == Default
                    else -> false
                }
            } ?: Default
        }

        /** Включает выбранный alias и выключает остальные (без убийства приложения; лаунчер обновит ярлык сам). */
        fun apply(ctx: Context, icon: AppIcon) {
            val pm = ctx.packageManager
            pm.setComponentEnabledSetting(icon.component(ctx), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(it.component(ctx), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }
}
