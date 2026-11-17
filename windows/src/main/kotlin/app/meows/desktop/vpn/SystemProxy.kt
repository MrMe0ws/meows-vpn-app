package app.meows.desktop.vpn

import app.meows.desktop.data.SettingsStore
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg

private interface WinInet : Library {
    fun InternetSetOptionW(h: Pointer?, option: Int, buffer: Pointer?, length: Int): Boolean
}

/**
 * Системный прокси Windows (HKCU\...\Internet Settings + оповещение WinInet). Прежние значения запоминаем в настройках
 * и возвращаем при отключении; если приложение упало с включённым прокси — восстанавливаем при следующем запуске.
 */
class SystemProxy(private val store: SettingsStore) {
    private val key = "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings"
    private val hkcu = WinReg.HKEY_CURRENT_USER
    private val wininet: WinInet? = runCatching { Native.load("wininet", WinInet::class.java) }.getOrNull()

    private fun str(name: String) = runCatching { Advapi32Util.registryGetStringValue(hkcu, key, name) }.getOrDefault("")
    private fun int(name: String) = runCatching { Advapi32Util.registryGetIntValue(hkcu, key, name) }.getOrDefault(0)

    fun enable(httpPort: Int) {
        if (!store.bool("proxyOwned", false)) {   // не затираем «прежние» значения при повторном включении
            store.set("prevEnable", int("ProxyEnable")); store.set("prevServer", str("ProxyServer")); store.set("prevOverride", str("ProxyOverride"))
        }
        store.set("proxyOwned", true)
        Advapi32Util.registrySetIntValue(hkcu, key, "ProxyEnable", 1)
        Advapi32Util.registrySetStringValue(hkcu, key, "ProxyServer", "127.0.0.1:$httpPort")
        Advapi32Util.registrySetStringValue(hkcu, key, "ProxyOverride", "localhost;127.*;10.*;172.16.*;172.17.*;172.18.*;172.19.*;172.2*;172.30.*;172.31.*;192.168.*;<local>")
        notifyChange()
    }

    fun restore() {
        if (!store.bool("proxyOwned", false)) return
        Advapi32Util.registrySetIntValue(hkcu, key, "ProxyEnable", store.int("prevEnable", 0))
        Advapi32Util.registrySetStringValue(hkcu, key, "ProxyServer", store.str("prevServer"))
        Advapi32Util.registrySetStringValue(hkcu, key, "ProxyOverride", store.str("prevOverride"))
        store.set("proxyOwned", false)
        notifyChange()
    }

    private fun notifyChange() {
        wininet?.InternetSetOptionW(null, 39, null, 0)   // INTERNET_OPTION_SETTINGS_CHANGED
        wininet?.InternetSetOptionW(null, 37, null, 0)   // INTERNET_OPTION_REFRESH
    }
}