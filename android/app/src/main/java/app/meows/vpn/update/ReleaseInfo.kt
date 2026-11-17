package app.meows.vpn.update

/** Релиз на GitHub с APK. */
data class ReleaseInfo(
    val version: String,     // без «v»: 2.1.0
    val notes: String,       // описание релиза (markdown как есть)
    val apkUrl: String,
    val apkSize: Long,       // байт, 0 если неизвестно
    val pageUrl: String,
)

/** Состояние обновления для интерфейса. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: ReleaseInfo) : UpdateState
    data class Downloading(val info: ReleaseInfo, val percent: Int) : UpdateState
    data class Downloaded(val info: ReleaseInfo, val path: String) : UpdateState
    data class Error(val message: String) : UpdateState
}
