package app.meows.desktop.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource

/** Картинки из src/main/resources. */
object Res {
    private val cache = HashMap<String, ImageBitmap>()
    fun bitmap(name: String): ImageBitmap = cache.getOrPut(name) { useResource(name, ::loadImageBitmap) }
}