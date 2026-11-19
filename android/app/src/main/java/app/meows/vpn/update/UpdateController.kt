package app.meows.vpn.update

import app.meows.vpn.i18n.trf
import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Логика обновлений приложения. Состояние читает интерфейс, команды отдаёт ViewModel. */
class UpdateController(
    private val app: Application,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
) {
    var state: UpdateState by mutableStateOf(UpdateState.Idle); private set
    /** Показывать диалог «Доступно обновление». */
    var dialogVisible by mutableStateOf(false); private set

    val currentVersion: String = runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty()
    }.getOrDefault("0")

    private var pending: ReleaseInfo? = null

    var autoCheck: Boolean
        get() = prefs.getBoolean(KEY_AUTO, true)
        set(v) { prefs.edit().putBoolean(KEY_AUTO, v).apply() }

    /** При запуске: только если включено, не чаще раза в 12 часов, и не для пропущенной версии. */
    fun checkOnLaunch() {
        if (!autoCheck) return
        val last = prefs.getLong(KEY_LAST, 0)
        if (System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return
        check(manual = false)
    }

    fun check(manual: Boolean) {
        if (state is UpdateState.Checking || state is UpdateState.Downloading) return
        state = UpdateState.Checking
        scope.launch {
            val r = withContext(Dispatchers.IO) { UpdateChecker.latest() }
            prefs.edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
            state = when (r) {
                is UpdateChecker.Result.Found ->
                    if (Version.isNewer(r.info.version, currentVersion)) UpdateState.Available(r.info) else UpdateState.UpToDate
                UpdateChecker.Result.NoReleases -> UpdateState.UpToDate
                is UpdateChecker.Result.Failed -> if (manual) UpdateState.Error(r.message) else UpdateState.Idle
            }
            val s = state
            if (s is UpdateState.Available) pending = s.info
            if (s is UpdateState.Available) dialogVisible = true
        }
    }

    fun download() {
        val info = pending ?: return
        if (state is UpdateState.Downloading) return
        state = UpdateState.Downloading(info, 0)
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    UpdateInstaller.download(app, info) { p -> state = UpdateState.Downloading(info, p) }
                }
            }
            state = result.fold(
                onSuccess = { UpdateState.Downloaded(info, it.absolutePath) },
                onFailure = { UpdateState.Error(trf("Не удалось скачать: %s", it.message ?: it.javaClass.simpleName)) },
            )
        }
    }

    /** Возвращает false, если сначала нужно разрешение на установку (открыты настройки). */
    fun install(): Boolean {
        val s = state as? UpdateState.Downloaded ?: return true
        if (UpdateInstaller.needsInstallPermission(app)) {
            UpdateInstaller.openInstallPermissionSettings(app)
            return false
        }
        UpdateInstaller.install(app, File(s.path))
        return true
    }

    fun dismissDialog() { dialogVisible = false }
    fun showDialog() { if (state is UpdateState.Available || state is UpdateState.Downloaded) dialogVisible = true }
    fun clearError() { if (state is UpdateState.Error) state = UpdateState.Idle }

    companion object {
        private const val KEY_AUTO = "updateCheck"
        private const val KEY_LAST = "updateLastCheck"
        private const val CHECK_INTERVAL_MS = 12 * 3_600_000L
    }
}
