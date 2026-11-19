package app.meows.desktop.ui.theme

import app.meows.desktop.i18n.tr
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/** Палитры те же, что в Android-версии, и в том же порядке (индекс хранится в настройках). «Мяу-зелёный» (0) — по умолчанию. */
object Palettes {
    val names: List<String> get() = listOf(tr("Мяу-зелёный"), tr("Бирюза"), tr("Лаванда"), tr("Закат"), tr("Роза"), tr("Океан"), tr("Серая"), tr("Монохром"), tr("Лес"), tr("Шалфей"), tr("Лапка"))
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