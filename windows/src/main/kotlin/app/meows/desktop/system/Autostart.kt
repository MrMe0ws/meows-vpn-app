package app.meows.desktop.system

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg

/** Автозапуск при входе в Windows: HKCU\...\Run, запуск сразу в трей (`--minimized`). */
object Autostart {
    private const val KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val NAME = "Meows"

    fun isEnabled(): Boolean = runCatching { Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, KEY, NAME) }.getOrDefault(false)

    fun set(enabled: Boolean) {
        runCatching {
            if (enabled) {
                val exe = ExePath.current() ?: return
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, KEY, NAME, "\"$exe\" --minimized")
            } else if (isEnabled()) Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, KEY, NAME)
        }
    }
}