package app.meows.vpn.routing

import android.util.Base64
import app.meows.vpn.XKStore
import org.json.JSONObject

/**
 * Профили маршрутизации (формат Happ: списки «напрямую / через VPN / блок», порядок, стратегия доменов, DNS).
 * «Из подписки» — профиль не выбран, действуют правила шаблона подписки (как было всегда). Остальные профили
 * ядро применяет вместо правил подписки (`core/profile.go`); личные надстройки приложения остаются выше.
 * Выбранный профиль хранится в pref `routeProfile`: `sub`, `meows`, `roscom` или `c:<id>` — свой (см. [CustomProfiles]).
 * Встроенные профили можно править: правка хранится отдельно (`routeBuiltin`) и снимается кнопкой «Сбросить».
 */
object RoutingProfiles {
    const val SUBSCRIPTION = "sub"
    const val MEOWS = "meows"
    const val ROSCOM = "roscom"
    val BUILTIN_IDS = listOf(MEOWS, ROSCOM)
    private const val OVERRIDES = "routeBuiltin"
    private const val CUSTOM = "c:"

    /**
     * Встроенный «Мяус» — типовой шаблон магазина (набор правил, общий для основных серверов подписки: Германия, Эстония, Швеция,
     * Финляндия, Россия), снятый с реальной подписки. Теги есть во вшитых гео-файлах. DNS — как в конфиге шаблона.
     */
    private val meows = RouteProfile(
        id = MEOWS, name = "Мяус",
        directSites = listOf(
            "geosite:private", "geosite:category-ru", "geosite:whitelist", "geosite:microsoft", "geosite:apple", "geosite:epicgames",
            "geosite:riot", "geosite:escapefromtarkov", "geosite:steam", "geosite:twitch", "geosite:pinterest", "geosite:faceit",
        ),
        directIp = listOf("77.88.8.8", "geoip:private", "geoip:direct"),
        proxySites = listOf(
            "geosite:google-play", "geosite:github", "geosite:google-deepmind", "geosite:twitch-ads", "geosite:youtube",
            "domain:anthropic.com", "domain:claude.ai", "domain:claudeusercontent.com",
            "domain:cursor.com", "domain:cursor.sh", "domain:cursorapi.com", "domain:cursor-cdn.com", "domain:cursorvm.com",
            "domain:grok.com", "domain:x.ai", "domain:grok.x.com",
            "domain:openai.com", "domain:chatgpt.com", "domain:oaistatic.com", "domain:oaiusercontent.com", "domain:copilot.microsoft.com",
            "domain:instagram.com", "domain:cdninstagram.com", "domain:ig.me",
            "domain:facebook.com", "domain:facebook.net", "domain:fbcdn.net", "domain:fb.com", "domain:meta.com", "domain:messenger.com",
            "domain:whatsapp.com", "domain:whatsapp.net",
        ),
        proxyIp = listOf("1.1.1.1", "8.8.8.8"),
        blockSites = listOf("geosite:win-spy", "geosite:category-ads"),
        remoteType = "DoH", remoteDomain = "https://1.1.1.1/dns-query", remoteIp = "1.1.1.1",
        localType = "DoH", localDomain = "https://77.88.8.8/dns-query", localIp = "77.88.8.8",
        dnsHosts = mapOf("lkfl2.nalog.ru" to "213.24.64.175", "lknpd.nalog.ru" to "213.24.64.181"),
    )

    /** Профиль RoscomVPN (`HAPP/DEFAULT.JSON` из hydraponique/roscomvpn-routing): РФ напрямую, YouTube, Telegram, GitHub через VPN, реклама и слежка блокируются. */
    private val roscom = RouteProfile(
        id = ROSCOM, name = "RoscomVPN",
        directSites = listOf(
            "geosite:private", "geosite:category-ru", "geosite:whitelist", "geosite:microsoft", "geosite:apple", "geosite:epicgames",
            "geosite:riot", "geosite:escapefromtarkov", "geosite:steam", "geosite:twitch", "geosite:pinterest", "geosite:faceit",
        ),
        directIp = listOf("geoip:private", "geoip:direct"),
        proxySites = listOf("geosite:google-play", "geosite:github", "geosite:twitch-ads", "geosite:youtube", "geosite:telegram"),
        blockSites = listOf("geosite:win-spy", "geosite:torrent", "geosite:category-ads"),
        remoteType = "DoH", remoteDomain = "https://8.8.8.8/dns-query", remoteIp = "8.8.8.8",
        localType = "DoH", localDomain = "https://77.88.8.8/dns-query", localIp = "77.88.8.8",
        dnsHosts = mapOf("lkfl2.nalog.ru" to "213.24.64.175", "lknpd.nalog.ru" to "213.24.64.181"),
        geoipUrl = "https://cdn.jsdelivr.net/gh/hydraponique/roscomvpn-geoip@202611201128/release/geoip.dat",
        geositeUrl = "https://cdn.jsdelivr.net/gh/hydraponique/roscomvpn-geosite@202610120948/release/geosite.dat",
    )

    /** Исходный (заводской) вид встроенного профиля. */
    fun builtinDefault(id: String): RouteProfile = if (id == ROSCOM) roscom else meows

    private fun overrides(st: XKStore): JSONObject = runCatching { JSONObject(st.prefs().getString(OVERRIDES, "{}") ?: "{}") }.getOrDefault(JSONObject())

    fun isOverridden(st: XKStore, id: String) = overrides(st).has(id)

    /** Встроенный профиль с учётом правок пользователя. */
    fun builtin(st: XKStore, id: String): RouteProfile =
        overrides(st).optString(id).takeIf { it.isNotEmpty() }?.let { RouteProfile.fromJson(id, it) }?.copy(name = builtinDefault(id).name) ?: builtinDefault(id)

    fun saveBuiltin(st: XKStore, p: RouteProfile) {
        st.prefs().edit().putString(OVERRIDES, overrides(st).put(p.id, p.toJson()).toString()).apply()
    }

    fun resetBuiltin(st: XKStore, id: String) {
        st.prefs().edit().putString(OVERRIDES, overrides(st).apply { remove(id) }.toString()).apply()
    }

    fun customId(p: RouteProfile) = CUSTOM + p.id

    fun selectedId(st: XKStore): String {
        val id = st.prefs().getString(XKStore.P_ROUTE_PROFILE, SUBSCRIPTION) ?: SUBSCRIPTION
        return when {
            id in BUILTIN_IDS -> id
            id.startsWith(CUSTOM) && CustomProfiles.get(st, id.removePrefix(CUSTOM)) != null -> id
            else -> SUBSCRIPTION   // удалённый или неизвестный профиль — правила подписки
        }
    }

    /** Выбранный профиль; null — «Из подписки». */
    fun selectedProfile(st: XKStore): RouteProfile? = when (val id = selectedId(st)) {
        SUBSCRIPTION -> null
        MEOWS, ROSCOM -> builtin(st, id)
        else -> CustomProfiles.get(st, id.removePrefix(CUSTOM))
    }

    /** JSON выбранного профиля для ядра; пусто — правила подписки. */
    fun selectedJson(st: XKStore): String = when (val id = selectedId(st)) {
        SUBSCRIPTION -> ""
        MEOWS, ROSCOM -> builtin(st, id).toJson()
        else -> CustomProfiles.get(st, id.removePrefix(CUSTOM))?.toJson() ?: ""
    }

    /** JSON из заголовка `routing` подписки (`happ://routing/add/<base64 JSON>`), если панель его прислала. */
    private fun subscriptionObject(st: XKStore): JSONObject? = runCatching {
        val b64 = st.routingHeader().trim().substringAfterLast('/')
        JSONObject(String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8))
    }.getOrNull()

    fun subscriptionName(st: XKStore): String? = subscriptionObject(st)?.optString("Name")?.trim()?.ifEmpty { null }

    fun title(st: XKStore, id: String, subscriptionTitle: String, meowsTitle: String): String = when {
        id == SUBSCRIPTION -> subscriptionName(st) ?: subscriptionTitle
        id == MEOWS -> meowsTitle
        id == ROSCOM -> roscom.name
        else -> CustomProfiles.get(st, id.removePrefix(CUSTOM))?.name ?: subscriptionTitle
    }

    /**
     * Профиль подписки в виде списков: имя, DNS и гео-ссылки — из заголовка `routing`, правила — из Xray-конфига выбранного сервера
     * (там настоящий шаблон, см. [XrayRules]; у разных серверов он может отличаться). Нет ни того ни другого — встроенный «Мяус».
     */
    fun subscriptionAsProfile(st: XKStore): RouteProfile {
        val base = subscriptionObject(st)?.let { RouteProfile.fromJson("", it.toString()) } ?: RouteProfile()
        val derived = XrayRules.config(st.body(), st.index())?.let { XrayRules.profileFrom(it, base) }
        return derived ?: base.takeIf { it.ruleCount > 0 } ?: builtin(st, MEOWS)
    }

    /** Копия профиля как свой; имя получает приписку [suffix]. */
    fun copyAsCustom(st: XKStore, base: RouteProfile, name: String, suffix: String): RouteProfile =
        CustomProfiles.save(st, base.copy(id = "", name = "${name.ifBlank { base.name }} $suffix".trim()))
}
