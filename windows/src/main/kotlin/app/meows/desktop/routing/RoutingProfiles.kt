package app.meows.desktop.routing

import app.meows.desktop.data.SettingsStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.UUID

/**
 * Профили маршрутизации (формат Happ), как на Android (`android/.../routing/RoutingProfiles.kt` — встроенные профили там и здесь
 * должны совпадать). «Из подписки» — профиль не передаётся, действуют правила шаблона подписки. Остальные ядро применяет
 * вместо правил подписки (`core/profile.go`); личные надстройки остаются выше.
 * Выбранный профиль — setting `routeProfile`: `sub`, `meows`, `roscom` или `c:<id>` — свой. Свои профили — `routeProfiles`,
 * правки встроенных — `routeBuiltin`.
 */
object RoutingProfiles {
    const val SUBSCRIPTION = "sub"
    const val MEOWS = "meows"
    const val ROSCOM = "roscom"
    val BUILTIN_IDS = listOf(MEOWS, ROSCOM)
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

    /** `HAPP/DEFAULT.JSON` из hydraponique/roscomvpn-routing. */
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

    fun customId(p: RouteProfile) = CUSTOM + p.id
    fun builtinDefault(id: String): RouteProfile = if (id == ROSCOM) roscom else meows

    // ---------- правки встроенных ----------
    private fun overrides(st: SettingsStore): JSONObject = runCatching { JSONObject(st.str("routeBuiltin", "{}")) }.getOrDefault(JSONObject())
    fun isOverridden(st: SettingsStore, id: String) = overrides(st).has(id)
    fun builtin(st: SettingsStore, id: String): RouteProfile =
        overrides(st).optString(id).takeIf { it.isNotEmpty() }?.let { RouteProfile.fromJson(id, it) }?.copy(name = builtinDefault(id).name) ?: builtinDefault(id)
    fun saveBuiltin(st: SettingsStore, p: RouteProfile) = st.set("routeBuiltin", overrides(st).put(p.id, p.toJson()).toString())
    fun resetBuiltin(st: SettingsStore, id: String) = st.set("routeBuiltin", overrides(st).apply { remove(id) }.toString())

    // ---------- свои профили ----------
    fun customs(st: SettingsStore): List<RouteProfile> = runCatching {
        val a = JSONArray(st.str("routeProfiles", "[]"))
        (0 until a.length()).mapNotNull { i -> a.getJSONObject(i).let { RouteProfile.fromJson(it.getString("id"), it.getString("json")) } }
    }.getOrDefault(emptyList())

    fun custom(st: SettingsStore, id: String): RouteProfile? = customs(st).firstOrNull { it.id == id }

    /** Сохраняет профиль (новый — в конец); пустой id заменяется новым. Возвращает сохранённый. */
    fun saveCustom(st: SettingsStore, p: RouteProfile): RouteProfile {
        val saved = if (p.id.isEmpty()) p.copy(id = UUID.randomUUID().toString().take(8)) else p
        val all = customs(st)
        writeCustoms(st, if (all.any { it.id == saved.id }) all.map { if (it.id == saved.id) saved else it } else all + saved)
        return saved
    }

    fun deleteCustom(st: SettingsStore, id: String) = writeCustoms(st, customs(st).filter { it.id != id })

    private fun writeCustoms(st: SettingsStore, all: List<RouteProfile>) {
        val a = JSONArray()
        all.forEach { a.put(JSONObject().put("id", it.id).put("json", it.toJson())) }
        st.set("routeProfiles", a.toString())
    }

    /** Копия профиля как свой; имя получает приписку [suffix]. */
    fun copyAsCustom(st: SettingsStore, base: RouteProfile, name: String, suffix: String): RouteProfile =
        saveCustom(st, base.copy(id = "", name = "${name.ifBlank { base.name }} $suffix".trim()))

    // ---------- выбор ----------
    /** Неизвестный или удалённый профиль — правила подписки. */
    fun selectedId(st: SettingsStore): String {
        val id = st.str("routeProfile", SUBSCRIPTION)
        return when {
            id in BUILTIN_IDS -> id
            id.startsWith(CUSTOM) && custom(st, id.removePrefix(CUSTOM)) != null -> id
            else -> SUBSCRIPTION
        }
    }

    /** Выбранный профиль; null — «Из подписки». */
    fun selectedProfile(st: SettingsStore): RouteProfile? = when (val id = selectedId(st)) {
        SUBSCRIPTION -> null
        MEOWS, ROSCOM -> builtin(st, id)
        else -> custom(st, id.removePrefix(CUSTOM))
    }

    /** JSON выбранного профиля для ядра; пусто — правила подписки. */
    fun selectedJson(st: SettingsStore): String = when (val id = selectedId(st)) {
        SUBSCRIPTION -> ""
        MEOWS, ROSCOM -> builtin(st, id).toJson()
        else -> custom(st, id.removePrefix(CUSTOM))?.toJson() ?: ""
    }

    // ---------- профиль из подписки ----------
    /** JSON из заголовка `routing` подписки (`happ://routing/add/<base64 JSON>`), если панель его прислала. */
    private fun subscriptionObject(header: String?): JSONObject? = runCatching {
        val b64 = header!!.trim().substringAfterLast('/')
        JSONObject(String(Base64.getMimeDecoder().decode(b64), Charsets.UTF_8))
    }.getOrNull()

    fun subscriptionName(header: String?): String? = subscriptionObject(header)?.optString("Name")?.trim()?.ifEmpty { null }

    /**
     * Профиль подписки в виде списков: имя, DNS и гео-ссылки — из заголовка `routing`, правила — из Xray-конфига сервера [index]
     * (там настоящий шаблон, см. [XrayRules]; у разных серверов он может отличаться). Нет ни того ни другого — встроенный «Мяус».
     */
    fun subscriptionAsProfile(st: SettingsStore, header: String?, body: String?, index: Int): RouteProfile {
        val base = subscriptionObject(header)?.let { RouteProfile.fromJson("", it.toString()) } ?: RouteProfile()
        val derived = body?.let { XrayRules.config(it, index) }?.let { XrayRules.profileFrom(it, base) }
        return derived ?: base.takeIf { it.ruleCount > 0 } ?: builtin(st, MEOWS)
    }

    fun title(st: SettingsStore, id: String, header: String?, subscriptionTitle: String): String = when {
        id == SUBSCRIPTION -> subscriptionName(header) ?: subscriptionTitle
        id in BUILTIN_IDS -> builtinDefault(id).name
        else -> custom(st, id.removePrefix(CUSTOM))?.name ?: subscriptionTitle
    }
}
