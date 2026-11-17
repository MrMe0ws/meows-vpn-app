package app.meows.desktop.data

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.security.MessageDigest

/** Стабильный идентификатор устройства для лимита устройств в панели: хеш MachineGuid, не меняется при переустановке. */
object Hwid {
    val value: String by lazy {
        val guid = runCatching {
            Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Cryptography", "MachineGuid")
        }.getOrDefault(System.getenv("COMPUTERNAME") ?: "unknown")
        MessageDigest.getInstance("SHA-256").digest("meows:$guid".toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
    }
}