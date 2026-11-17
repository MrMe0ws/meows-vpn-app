package app.meows.desktop.tray

import java.awt.Image
import java.awt.MouseInfo
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO

/**
 * Иконка в трее на «голом» AWT: левый клик — открыть окно, правый — наше меню ([app.meows.desktop.ui.tray.TrayMenuWindow]).
 * Стандартное меню Compose `Tray` (AWT PopupMenu) выглядит как из Windows 95 и не подстраивается под тему.
 */
class TrayIconController(private val onOpen: () -> Unit, private val onMenu: (x: Int, y: Int) -> Unit) {
    private var icon: TrayIcon? = null
    private var pendingUrl: String? = null   // куда вести по щелчку на всплывающем уведомлении

    /** true — иконка добавлена (трей поддерживается). */
    fun install(tooltip: String): Boolean {
        if (!SystemTray.isSupported()) return false
        return runCatching {
            val tray = SystemTray.getSystemTray()
            val src = javaClass.getResourceAsStream("/icon.png")!!.use { ImageIO.read(it) }
            val size = tray.trayIconSize
            val img = src.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH)
            val ti = TrayIcon(img, tooltip)
            ti.isImageAutoSize = true
            ti.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { if (e.button == MouseEvent.BUTTON1) onOpen() }
                override fun mousePressed(e: MouseEvent) { if (e.isPopupTrigger) popup() }
                override fun mouseReleased(e: MouseEvent) { if (e.isPopupTrigger) popup() }
            })
            ti.addActionListener { val u = pendingUrl; pendingUrl = null; if (u != null) runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(u)) } else onOpen() }
            tray.add(ti); icon = ti
            true
        }.getOrDefault(false)
    }

    /** Всплывающее уведомление у иконки; щелчок по нему открывает [url] (если задан). */
    fun notify(title: String, text: String, url: String?) { pendingUrl = url; icon?.displayMessage(title, text, TrayIcon.MessageType.INFO) }

    private fun popup() { val p = MouseInfo.getPointerInfo().location; onMenu(p.x, p.y) }

    fun remove() { icon?.let { runCatching { SystemTray.getSystemTray().remove(it) } }; icon = null }
}
