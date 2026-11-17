package app.meows.vpn.shop

import android.content.Context
import android.os.Build
import app.meows.vpn.XKStore
import app.meows.vpn.XKVpnService
import app.meows.vpn.ui.components.formatBytesRu

/** Короткий отчёт для поддержки: без ссылки подписки, токенов и HWID; есть расход трафика. */
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
            appendLine("Meows $version")
            appendLine("Android ${Build.VERSION.RELEASE}, ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Сервер: $server")
            appendLine("Подписка: $sub")
            appendLine("VPN: $vpn")
            val total = st.total()
            appendLine("Трафик: ${formatBytesRu(st.used())} из " + if (total > 0) formatBytesRu(total) else "без лимита")
            if (!lastError.isNullOrBlank()) appendLine("Последняя ошибка: ${lastError.trim()}")
        }.trim()
    }
}
