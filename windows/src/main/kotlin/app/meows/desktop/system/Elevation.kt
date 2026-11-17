package app.meows.desktop.system

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString

private interface Shell32Api : Library {
    fun IsUserAnAdmin(): Boolean
    fun ShellExecuteW(hwnd: Pointer?, operation: WString, file: WString, params: WString?, dir: WString?, show: Int): Pointer?
}

/** Права администратора: нужны режиму TUN (создание сетевого адаптера и маршрутов). */
object Elevation {
    private val shell: Shell32Api? = runCatching { Native.load("shell32", Shell32Api::class.java) }.getOrNull()

    fun isAdmin(): Boolean = runCatching { shell?.IsUserAnAdmin() }.getOrNull() ?: false

    /**
     * Запускает это же приложение с правами администратора (окно UAC). Новый процесс подождёт, пока завершится текущий
     * (`--wait-pid`), чтобы занять «единственный экземпляр». true — запрос подтверждён, текущему нужно закрыться.
     */
    fun relaunch(extraArgs: List<String>): Boolean {
        val exe = ExePath.current() ?: return false
        val pid = ProcessHandle.current().pid()
        val params = (listOf("--wait-pid", pid.toString()) + extraArgs).joinToString(" ") { if (' ' in it || ':' in it) "\"$it\"" else it }
        val r = runCatching { shell?.ShellExecuteW(null, WString("runas"), WString(exe), WString(params), null, 1) }.getOrNull()
        return r != null && Pointer.nativeValue(r) > 32   // ≤ 32 — ошибка или отказ в UAC
    }
}
