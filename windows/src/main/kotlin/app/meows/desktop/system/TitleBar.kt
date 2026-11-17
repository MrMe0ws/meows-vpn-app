package app.meows.desktop.system

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference

private interface Dwm : Library {
    fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: IntByReference, size: Int): Int
}

/** Цвет заголовка окна Windows 11 (DWM): подкрашиваем под тему приложения. На старых Windows молча ничего не делает. */
object TitleBar {
    private val dwm: Dwm? = runCatching { Native.load("dwmapi", Dwm::class.java) }.getOrNull()

    private fun colorRef(argb: Int) = ((argb shr 16) and 0xFF) or (argb and 0xFF00) or ((argb and 0xFF) shl 16)

    fun apply(handle: Long, captionArgb: Int, textArgb: Int, dark: Boolean) {
        if (handle == 0L) return
        val h = Pointer(handle)
        runCatching {
            dwm?.DwmSetWindowAttribute(h, 20, IntByReference(if (dark) 1 else 0), 4)    // DWMWA_USE_IMMERSIVE_DARK_MODE
            dwm?.DwmSetWindowAttribute(h, 35, IntByReference(colorRef(captionArgb)), 4) // DWMWA_CAPTION_COLOR
            dwm?.DwmSetWindowAttribute(h, 36, IntByReference(colorRef(textArgb)), 4)    // DWMWA_TEXT_COLOR
        }
    }
}
