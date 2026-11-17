package app.meows.desktop.update

/** Релиз на GitHub с установщиком Windows (.msi). */
data class ReleaseInfo(val version: String, val notes: String, val msiUrl: String, val size: Long)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: ReleaseInfo) : UpdateState
    data class Downloading(val info: ReleaseInfo, val percent: Int) : UpdateState
    data class Downloaded(val info: ReleaseInfo, val path: String) : UpdateState
    data class Error(val message: String) : UpdateState
}

object Version {
    private fun parse(v: String) = v.trim().removePrefix("v").substringBefore('-').split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate); val b = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val d = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (d != 0) return d > 0
        }
        return false
    }
}