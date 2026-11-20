package app.meows.vpn

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Варианты иконки приложения: на каждый — activity-alias в манифесте; включён ровно один. */
enum class AppIcon(val alias: String, val title: String, val preview: Int) {
    Lime("LauncherLime", "Лайм", R.drawable.icon_prev_lime),
    Green("LauncherGreen", "Зелёная лапа", R.drawable.icon_prev_green),
    Glow("LauncherGlow", "Свечение", R.drawable.icon_prev_glow),
    Classic("LauncherClassic", "Классика", R.drawable.icon_prev_classic);

    private fun component(ctx: Context) = ComponentName(ctx.packageName, "app.meows.vpn.$alias")

    companion object {
        /** Выбранная, но ещё не применённая иконка: смена компонента во время работы приложения выкидывает его на рабочий стол, поэтому применяем при сворачивании. */
        var pending: AppIcon? = null

        fun applyPending(ctx: Context) {
            val p = pending ?: return
            pending = null
            apply(ctx, p)
        }

        fun current(ctx: Context): AppIcon = pending ?: currentApplied(ctx)

        fun currentApplied(ctx: Context): AppIcon {
            val pm = ctx.packageManager
            return entries.firstOrNull { icon ->
                when (pm.getComponentEnabledSetting(icon.component(ctx))) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == Lime
                    else -> false
                }
            } ?: Lime
        }

        /** Если ни один alias не включён (например, после смены набора alias-ов) — включает «Лайм», иначе иконка пропадёт из лаунчера. */
        fun ensureOne(ctx: Context) {
            val pm = ctx.packageManager
            val any = entries.any {
                when (pm.getComponentEnabledSetting(it.component(ctx))) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> it == Lime
                    else -> false
                }
            }
            if (!any) apply(ctx, Lime)
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
