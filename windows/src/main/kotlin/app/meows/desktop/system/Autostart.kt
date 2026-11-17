package app.meows.desktop.system

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg

/**
 * Автозапуск при входе в Windows, сразу в трей (`--minimized`).
 * С правами администратора — задача планировщика «с наивысшими правами» (стартует без окна UAC, нужно для TUN);
 * без них — запись в HKCU\...\Run.
 */
object Autostart {
    private const val KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val NAME = "Meows"

    private fun runKey() = runCatching { Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, KEY, NAME) }.getOrDefault(false)

    private fun ps(cmd: String): Int = runCatching {
        val p = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", cmd).redirectErrorStream(true).start()
        p.inputStream.readBytes()
        p.waitFor()
    }.getOrDefault(-1)

    private fun task() = ps("if (Get-ScheduledTask -TaskName $NAME -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }") == 0

    fun isEnabled(): Boolean = runKey() || task()

    fun set(enabled: Boolean) {
        runCatching {
            if (!enabled) {
                if (runKey()) Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, KEY, NAME)
                if (Elevation.isAdmin()) ps("Unregister-ScheduledTask -TaskName $NAME -Confirm:\$false -ErrorAction SilentlyContinue")
                return
            }
            val exe = ExePath.current() ?: return
            if (Elevation.isAdmin()) {
                val code = ps(
                    "\$a = New-ScheduledTaskAction -Execute '$exe' -Argument '--minimized'; " +
                        "\$t = New-ScheduledTaskTrigger -AtLogOn -User \$env:USERNAME; " +
                        "\$p = New-ScheduledTaskPrincipal -UserId \$env:USERNAME -RunLevel Highest -LogonType Interactive; " +
                        "\$s = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero); " +
                        "Register-ScheduledTask -TaskName $NAME -Action \$a -Trigger \$t -Principal \$p -Settings \$s -Force | Out-Null",
                )
                if (code == 0) {
                    if (runKey()) Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, KEY, NAME)
                    return
                }
            }
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, KEY, NAME, "\"$exe\" --minimized")
        }
    }
}
