package app.meows.vpn.shop

import app.meows.vpn.XKStore

/**
 * Ссылки «Кабинет» и «Поддержка» — из заголовков подписки (их задаёт панель Remnawave: `profile-web-page-url`, `support-url`),
 * то есть у каждого VPN-сервиса свои. Запасные адреса Meows подставляем только для подписок Meows (домен me0ws.store);
 * у чужой подписки без заголовка ссылки нет (null) — кнопку не показываем.
 */
object ShopLinks {
    const val CABINET_FALLBACK = "https://cabinet.me0ws.store"
    const val SUPPORT_FALLBACK = "https://t.me/VPNmeows_bot"

    fun isMeows(st: XKStore): Boolean = runCatching { java.net.URI(st.url()).host.orEmpty().endsWith("me0ws.store") }.getOrDefault(false)

    @JvmStatic fun cabinet(st: XKStore): String? = st.webUrl()?.takeIf { it.isNotBlank() } ?: CABINET_FALLBACK.takeIf { isMeows(st) }
    fun support(st: XKStore): String? = st.supportUrl()?.takeIf { it.isNotBlank() } ?: SUPPORT_FALLBACK.takeIf { isMeows(st) }

    /** Дней до конца подписки: null — срок неизвестен, отрицательное — уже закончилась. */
    fun daysLeft(st: XKStore): Int? {
        val expire = st.expire() * 1000L
        if (expire <= 0) return null
        val diff = expire - System.currentTimeMillis()
        return if (diff < 0) -1 else (diff / 86_400_000L).toInt()
    }

    /** Пора предложить продление: осталось ≤ 7 дней или подписка закончилась. */
    fun needsRenewal(st: XKStore): Boolean = daysLeft(st)?.let { it <= 7 } ?: false
}
