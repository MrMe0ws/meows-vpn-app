package app.meows.desktop.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.DropdownMenuItem as M3DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton as M3FilledTonalButton
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.NavigationRailItem as M3NavigationRailItem
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.SegmentedButton as M3SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Switch as M3Switch
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.material3.Button as M3Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.graphics.Shape

/**
 * На десктопе у кликабельных элементов курсор должен становиться «рукой». Material 3 этого сам не делает,
 * поэтому приложение берёт кнопки и пр. отсюда (те же имена и параметры, что в material3) — правило одно на всё приложение.
 */
fun Modifier.hand(enabled: Boolean = true): Modifier = if (enabled) pointerHoverIcon(PointerIcon.Hand) else this

@Composable
fun Button(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    M3Button(onClick = onClick, modifier = modifier.hand(enabled), enabled = enabled, content = content)

@Composable
fun FilledTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    M3FilledTonalButton(onClick = onClick, modifier = modifier.hand(enabled), enabled = enabled, content = content)

@Composable
fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    M3OutlinedButton(onClick = onClick, modifier = modifier.hand(enabled), enabled = enabled, content = content)

@Composable
fun TextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    M3TextButton(onClick = onClick, modifier = modifier.hand(enabled), enabled = enabled, content = content)

@Composable
fun IconButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(), content: @Composable () -> Unit,
) = M3IconButton(onClick = onClick, modifier = modifier.hand(enabled), enabled = enabled, colors = colors, content = content)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier) =
    M3FilterChip(selected = selected, onClick = onClick, label = label, modifier = modifier.hand())

@Composable
fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) =
    M3Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = if (enabled) modifier.hand() else modifier, enabled = enabled)

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null, trailingIcon: @Composable (() -> Unit)? = null,
) = M3DropdownMenuItem(text = text, onClick = onClick, modifier = modifier.hand(), leadingIcon = leadingIcon, trailingIcon = trailingIcon)

@Composable
fun NavigationRailItem(
    selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit, modifier: Modifier = Modifier, label: @Composable (() -> Unit)? = null,
) = M3NavigationRailItem(selected = selected, onClick = onClick, icon = icon, modifier = modifier.hand(), label = label)

@Composable
fun SingleChoiceSegmentedButtonRowScope.SegmentedButton(selected: Boolean, onClick: () -> Unit, shape: Shape, modifier: Modifier = Modifier, label: @Composable () -> Unit) =
    M3SegmentedButton(selected = selected, onClick = onClick, shape = shape, modifier = modifier.hand(), label = label)
