package app.meows.vpn.ui.settings.pages

import app.meows.vpn.i18n.tr
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.AppIcon
import app.meows.vpn.ui.settings.ValueRow
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow
import app.meows.vpn.ui.theme.Palettes
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/** Тема (авто/светлая/тёмная), цвет (из обоев или палитра), AMOLED. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearancePage(vm: MeowsViewModel, onBack: () -> Unit, openAppIcon: () -> Unit) {
    SettingsPageScaffold(tr("Оформление"), onBack) {
        SettingsGroup(tr("Тема")) {
            val modes = listOf(tr("Авто"), tr("Светлая"), tr("Тёмная"))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                modes.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = vm.themeMode == i, onClick = { vm.setTheme(i) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    ) { Text(m) }
                }
            }
        }
        SettingsGroup(tr("Цвет"), tr("Цвета из обоев меняются вместе с обоями (Android 12+). Остальные палитры — фиксированные.")) {
            Column(Modifier.padding(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (Build.VERSION.SDK_INT >= 31) {
                        Swatch(MaterialTheme.colorScheme.tertiary, vm.dynamicColors, Icons.Rounded.Public) { vm.setDynamic(true) }
                    }
                    Palettes.seeds.forEachIndexed { i, c ->
                        Swatch(c, !vm.dynamicColors && vm.palette == i) { vm.choosePalette(i) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (vm.dynamicColors) tr("Цвета из обоев") else Palettes.names[vm.palette.coerceIn(0, Palettes.names.lastIndex)],
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SettingsGroup {
            SwitchRow(Icons.Rounded.Lock, tr("Чёрный фон (AMOLED)"), vm.amoled, tr("Для тёмной темы: экономит заряд на OLED-экранах")) { vm.chooseAmoled(it) }
        }
        SettingsGroup {
            ValueRow(Icons.Rounded.AppShortcut, tr("Иконка приложения"), summary = tr(AppIcon.current(LocalContext.current).title), onClick = openAppIcon)
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = CircleShape, color = color, modifier = Modifier.size(48.dp),
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Rounded.Check, tr("Выбрано"), tint = Color.Black.copy(alpha = 0.75f))
            else if (icon != null) Icon(icon, null, tint = Color.Black.copy(alpha = 0.6f))
        }
    }
}
