package app.meows.vpn.ui.settings.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.meows.vpn.AppIcon
import app.meows.vpn.i18n.tr

/** Выбор иконки приложения (внутри «Оформления»): сверху предпросмотр выбранной, ниже сетка вариантов; выбор применяется сразу. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppIconPicker() {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(AppIcon.current(ctx)) }
    Column {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(selected.preview), tr(selected.title), Modifier.size(112.dp).clip(CircleShape))
            Text(tr(selected.title), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
        }
        FlowRow(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon.entries.forEach { icon ->
                    val on = icon == selected
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).clickable { selected = icon; AppIcon.pending = if (icon == AppIcon.currentApplied(ctx)) null else icon }) {
                        Surface(
                            Modifier.size(72.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(if (on) 2.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Image(painterResource(icon.preview), tr(icon.title), Modifier.padding(6.dp).clip(CircleShape))
                        }
                        if (on) Icon(Icons.Rounded.CheckCircle, null, Modifier.align(Alignment.BottomEnd).size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
    }
}
