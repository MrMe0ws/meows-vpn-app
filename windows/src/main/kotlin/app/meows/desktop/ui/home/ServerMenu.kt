package app.meows.desktop.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import app.meows.desktop.ui.components.DropdownMenuItem
import kotlin.math.roundToInt

/** Правая кнопка мыши: позиция нажатия передаётся в [onOpen] (до дочерних элементов, чтобы кнопка строки не «съела» событие). */
fun Modifier.secondaryClick(onOpen: (IntOffset) -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val e = awaitPointerEvent(PointerEventPass.Initial)
            if (e.type == PointerEventType.Press && e.buttons.isSecondaryPressed) {
                val p = e.changes.first().position
                onOpen(IntOffset(p.x.roundToInt(), p.y.roundToInt()))
            }
        }
    }
}

/** Меню сервера по правой кнопке — открывается у курсора: пинг, выбрать, скопировать название. */
@Composable
fun ServerMenu(at: IntOffset?, title: String, onDismiss: () -> Unit, onPing: () -> Unit, onSelect: () -> Unit) {
    Box(Modifier.offset { at ?: IntOffset.Zero }) {
        DropdownMenu(at != null, onDismissRequest = onDismiss) {
            DropdownMenuItem(text = { Text("Проверить пинг") }, leadingIcon = { Icon(Icons.Rounded.Speed, null) }, onClick = { onDismiss(); onPing() })
            DropdownMenuItem(text = { Text("Выбрать сервер") }, leadingIcon = { Icon(Icons.Rounded.CheckCircle, null) }, onClick = { onDismiss(); onSelect() })
            DropdownMenuItem(text = { Text("Скопировать название") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = { onDismiss(); copyText(title) })
        }
    }
}
