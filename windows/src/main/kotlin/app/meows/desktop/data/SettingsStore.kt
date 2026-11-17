package app.meows.desktop.data

import org.json.JSONObject

/** Простое хранилище настроек в JSON-файле %APPDATA%\Meows\settings.json. */
class SettingsStore {
    private val file = AppDirs.file("settings.json")
    private val json = JSONObject(runCatching { file.readText() }.getOrDefault("{}"))

    @Synchronized fun int(k: String, d: Int) = json.optInt(k, d)
    @Synchronized fun bool(k: String, d: Boolean) = json.optBoolean(k, d)
    @Synchronized fun str(k: String, d: String = "") = json.optString(k, d)
    @Synchronized fun long(k: String, d: Long = 0) = json.optLong(k, d)
    @Synchronized fun set(k: String, v: Any?) { if (v == null) json.remove(k) else json.put(k, v); runCatching { file.writeText(json.toString(2)) } }
}