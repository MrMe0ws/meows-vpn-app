package app.meows.vpn.ui.theme

import app.meows.vpn.i18n.tr
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * Готовые палитры по seed-цвету. Порядок важен: в настройках хранится индекс (новые — только в конец).
 * 0 — «Мяу-зелёный» (глаза кота). 6–7 — нейтральные; 8–10 («Лес», «Шалфей», «Лапка») — спокойные, те же, что в Windows-версии.
 */
object Palettes {
    val names: List<String> get() = listOf(tr("Мяу-зелёный"), tr("Бирюза"), tr("Лаванда"), tr("Закат"), tr("Роза"), tr("Океан"), tr("Серая"), tr("Монохром"), tr("Лес"), tr("Шалфей"), tr("Лапка"), tr("Сумерки"))
    val seeds = listOf(
        Color(0xFF6FD321), Color(0xFF14C8D8), Color(0xFF8E7CF0),
        Color(0xFFFF8A3D), Color(0xFFE8638F), Color(0xFF3D8BFF),
        Color(0xFF8793A0), Color(0xFF8C8C8C),
        Color(0xFF4F8A5F), Color(0xFF7A9A82), Color(0xFF00C8C8), Color(0xFF5B52E6),
    )
    /** Насыщенность поверхностей тёмной темы по палитрам (0 — серые). У «Сумерек» (11) свои цвета, см. [indigoDark]. */
    val darkTint = listOf(28.0, 28.0, 30.0, 22.0, 26.0, 32.0, 6.0, 0.0, 14.0, 8.0, 22.0, 0.0)
    val styles = listOf(
        PaletteStyle.Vibrant, PaletteStyle.Vibrant, PaletteStyle.Vibrant,
        PaletteStyle.Vibrant, PaletteStyle.Vibrant, PaletteStyle.Vibrant,
        PaletteStyle.Neutral,    // серая: едва заметный холодный оттенок
        PaletteStyle.Monochrome, // монохром: чисто белый / серый / чёрный
        PaletteStyle.TonalSpot,  // «Лес»: спокойный зелёный
        PaletteStyle.Neutral,    // «Шалфей»: серо-зелёный
        PaletteStyle.TonalSpot,  // «Лапка»: бирюза прежней иконки (#00C8C8)
        PaletteStyle.TonalSpot,  // «Сумерки»: тёмно-синий и индиго, как в Happ (тёмная тема — свои цвета, см. ThemeLayers.kt)
    )
}