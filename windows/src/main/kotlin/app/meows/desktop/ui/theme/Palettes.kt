package app.meows.desktop.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/** Первые 8 палитр те же, что в Android-версии, в том же порядке (индекс хранится в настройках); дальше — только Windows. */
object Palettes {
    val names = listOf("Мяу-зелёный", "Бирюза", "Лаванда", "Закат", "Роза", "Океан", "Серая", "Монохром", "Лес", "Шалфей", "Лапка")
    val seeds = listOf(
        Color(0xFF6FD321), Color(0xFF14C8D8), Color(0xFF8E7CF0), Color(0xFFFF8A3D),
        Color(0xFFE8638F), Color(0xFF3D8BFF), Color(0xFF8793A0), Color(0xFF8C8C8C),
        Color(0xFF4F8A5F), Color(0xFF7A9A82), Color(0xFF00C8C8),
    )
    val styles = listOf(
        PaletteStyle.Vibrant, PaletteStyle.Vibrant, PaletteStyle.Vibrant, PaletteStyle.Vibrant,
        PaletteStyle.Vibrant, PaletteStyle.Vibrant, PaletteStyle.Neutral, PaletteStyle.Monochrome,
        PaletteStyle.TonalSpot, PaletteStyle.Neutral, PaletteStyle.TonalSpot,
    )
}