package app.meows.vpn.routing

import app.meows.vpn.XKStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Свои профили маршрутизации: массив `{id, json}` в prefs (`routeProfiles`). Профиль живёт в слоте подписки, как остальные настройки маршрутизации. */
object CustomProfiles {
    private const val KEY = "routeProfiles"

    fun list(st: XKStore): List<RouteProfile> = runCatching {
        val a = JSONArray(st.prefs().getString(KEY, "[]"))
        (0 until a.length()).mapNotNull { i -> a.getJSONObject(i).let { RouteProfile.fromJson(it.getString("id"), it.getString("json")) } }
    }.getOrDefault(emptyList())

    fun get(st: XKStore, id: String): RouteProfile? = list(st).firstOrNull { it.id == id }

    /** Сохраняет профиль (новый — в конец); пустой id заменяется новым. Возвращает сохранённый профиль. */
    fun save(st: XKStore, p: RouteProfile): RouteProfile {
        val saved = if (p.id.isEmpty()) p.copy(id = UUID.randomUUID().toString().take(8)) else p
        val all = list(st)
        val next = if (all.any { it.id == saved.id }) all.map { if (it.id == saved.id) saved else it } else all + saved
        write(st, next)
        return saved
    }

    fun delete(st: XKStore, id: String) = write(st, list(st).filter { it.id != id })

    private fun write(st: XKStore, all: List<RouteProfile>) {
        val a = JSONArray()
        all.forEach { a.put(JSONObject().put("id", it.id).put("json", it.toJson())) }
        st.prefs().edit().putString(KEY, a.toString()).apply()
    }
}
