package app.meows.vpn.shop

import android.content.Context
import android.os.Build
import app.meows.vpn.XKStore
import app.meows.vpn.XKVpnService

/** Короткий отчёт для поддержки: без ссылки подписки и токенов, HWID — только первые 8 символов. */
object Diagnostics {
    fun build(ctx: Context, st: XKStore, lastError: String?): String {
        val pi = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
        val version = pi?.let { "${it.versionName} (${it.longVersionCode})" } ?: "?"
        val server = st.servers().getOrNull(st.index())?.title ?: "—"
        val days = ShopLinks.daysLeft(st)
        val sub = when {
            !st.hasSubscription() -> "не добавлена"
            days == null -> "без срока"
            days < 0 -> "закончилась"
            else -> "осталось дней: $days"
        }
        val vpn = when (XKVpnService.state) {
            XKVpnService.CONNECTED -> "подключено"
            XKVpnService.CONNECTING -> "подключается"
            else -> "отключено"
        }
        return buildString {
            appendLine("Мяус VPN $version")
            appendLine("Android ${Build.VERSION.RELEASE}, ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Сервер: $server")
            appendLine("Подписка: $sub")
            appendLine("VPN: $vpn")
            appendLine("HWID: ${st.hwid().take(8)}…")
            if (!lastError.isNullOrBlank()) appendLine("Последняя ошибка: ${lastError.trim()}")
        }.trim()
    }
}
