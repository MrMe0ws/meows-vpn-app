package app.meows.desktop.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Одна подписка в списке: данные от панели, выбранный в ней сервер, закрепление, результаты пинга и «занята ли сейчас». */
class SubEntry(val id: Int, data: SubscriptionData, selected: Int = 0, pinned: Boolean = false) {
    var data by mutableStateOf(data)
    var selected by mutableIntStateOf(selected)
    var pinned by mutableStateOf(pinned)
    val pings = mutableStateMapOf<Int, Int>()
    val pingBusy = mutableStateListOf<Int>()     // серверы, у которых сейчас идёт пинг по одному
    var refreshing by mutableStateOf(false)
    var pinging by mutableStateOf(false)

    fun toJson(): JSONObject = JSONObject().put("id", id).put("sub", data.toJson()).put("selected", selected).put("pinned", pinned)

    companion object {
        /** Список из settings.json; прежний формат (одна подписка в ключах `sub` и `selected`) переносится без потерь. */
        fun load(store: SettingsStore): MutableList<SubEntry> {
            val out = ArrayList<SubEntry>()
            runCatching {
                val arr = JSONArray(store.str("subs", "[]"))
                for (i in 0 until arr.length()) {
                    val j = arr.getJSONObject(i)
                    val d = SubscriptionData.fromJson(j.getJSONObject("sub").toString()) ?: continue
                    out += SubEntry(j.getInt("id"), d, j.optInt("selected", 0), j.optBoolean("pinned", false))
                }
            }
            if (out.isEmpty()) SubscriptionData.fromJson(store.str("sub"))?.let { out += SubEntry(1, it, store.int("selected", 0)) }
            return out
        }

        fun save(store: SettingsStore, list: List<SubEntry>, activeId: Int) {
            store.set("subs", JSONArray().also { a -> list.forEach { a.put(it.toJson()) } }.toString())
            store.set("activeSub", activeId)
            store.set("sub", null); store.set("selected", null)   // старый формат больше не нужен
        }
    }
}
