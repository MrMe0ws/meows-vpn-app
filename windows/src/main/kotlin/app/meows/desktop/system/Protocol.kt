package app.meows.desktop.system

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg

/** Регистрирует meows:// для текущего пользователя (HKCU, без прав администратора): ссылка из кабинета открывает Meows и добавляет подписку. */
object Protocol {
    fun register() {
        val exe = ExePath.current() ?: return
        runCatching {
            val base = "Software\\Classes\\meows"
            Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, base)
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, base, "", "URL:Meows Protocol")
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, base, "URL Protocol", "")
            Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, "$base\\shell\\open\\command")
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "$base\\shell\\open\\command", "", "\"$exe\" \"%1\"")
        }
    }
}

/** Путь к Meows.exe (при запуске из Gradle — java.exe, тогда системные интеграции пропускаем). */
object ExePath {
    fun current(): String? = ProcessHandle.current().info().command().orElse(null)?.takeIf { it.endsWith("Meows.exe", ignoreCase = true) }
}