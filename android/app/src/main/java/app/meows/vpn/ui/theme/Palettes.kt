package app.meows.vpn.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * Готовые палитры по seed-цвету. Порядок важен: в настройках хранится индекс (новые — только в конец).
 * 0 — «Мяу-зелёный» (глаза кота, по умолчанию). Последние две — спокойные нейтральные для тех, кому яркое не по душе.
 */
object Palettes {
    val names = listOf("Мяу-зелёный", "Бирюза", "Лаванда", "Закат", "Роза", "Океан", "Серая", "Монохром")
    val seeds = listOf(
        Color(0xFF6FD321), Color(0xFF14C8D8), Color(0xFF8E7CF0),
        Color(0xFFFF8A3D), Color(0xFFE8638F), Color(0xFF3D8BFF),
        Color(0xFF8793A0), Color(0xFF8C8C8C),
    )
    val styles = listOf(
        PaletteStyle.Vibrant, PaletteStyle.Vibrant, PaletteStyle.Vibrant,
        PaletteStyle.Vibrant, PaletteStyle.Vibrant, PaletteStyle.Vibrant,
        PaletteStyle.Neutral,    // серая: едва заметный холодный оттенок
        PaletteStyle.Monochrome, // монохром: чисто белый / серый / чёрный
    )
}