package app.meows.desktop.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.meows.desktop.i18n.tr
import app.meows.desktop.ui.components.hand

/** Редактор открыт только для просмотра (профиль подписки): поля и переключатели недоступны. */
internal val LocalEditReadOnly = compositionLocalOf { false }

/** Заголовок раздела редактора (как у групп настроек). */
@Composable
internal fun EditTitle(text: String) = Text(
    text.uppercase(), Modifier.padding(start = 28.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
)

@Composable
internal fun EditCard(content: @Composable () -> Unit) = Surface(
    Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer,
) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() } }

/** Сворачиваемая карточка со своим заголовком (Напрямую / Через VPN / Блокировать). */
@Composable
internal fun FoldCard(title: String, tint: Color, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Surface(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            Row(Modifier.fillMaxWidth().clickable { open = !open }.hand().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = tint)
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(open) { Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() } }
        }
    }
}

/** Цвет примера в пустом поле: заметно бледнее введённого текста, чтобы пример не принимали за заполненное значение. */
@Composable
private fun hintColor() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

/** Подпись над полем: пример в самом поле виден, пока оно пусто. */
@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(label, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    content()
}

/** Многострочное поле «один элемент на строку»; пример из двух строк виден серым, пока поле пусто. */
@Composable
internal fun ListField(label: String, value: String, example: String, onChange: (String) -> Unit) = Labeled(label) {
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), readOnly = LocalEditReadOnly.current,
        placeholder = { Text(example, fontFamily = FontFamily.Monospace, color = hintColor()) },
        supportingText = { Text(tr("Один элемент на строку")) }, minLines = 3,
    )
}

@Composable
internal fun TextRow(label: String, value: String, onChange: (String) -> Unit, placeholder: String = "") = Labeled(label) {
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), singleLine = true, readOnly = LocalEditReadOnly.current,
        placeholder = if (placeholder.isEmpty()) null else { { Text(placeholder, color = hintColor()) } },
    )
}

/** DNS-сервер: тип (DoH / DoU / TCP) и адрес. DoH — ссылка, остальные — IP или имя сервера. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DnsPicker(title: String, tint: Color, type: String, onType: (String) -> Unit, domain: String, onDomain: (String) -> Unit, ip: String, onIp: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = tint)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("DoH", "DoU", "TCP").forEach { t -> FilterChip(type == t, { onType(t) }, label = { Text(t) }, modifier = Modifier.hand(), enabled = !LocalEditReadOnly.current) }
    }
    if (type == "DoH") TextRow(tr("Ссылка (DoH)"), domain, onDomain, "https://cloudflare-dns.com/dns-query")
    else TextRow(tr("Адрес DNS-сервера"), ip, onIp, "8.8.8.8")
}

/** Выбор одного из вариантов с радио (порядок правил, стратегия доменов). Подпись — необязательная. */
@Composable
internal fun RadioChoice(title: String, options: List<Triple<String, String, String?>>, selected: String, onPick: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    options.forEach { (key, name, desc) ->
        val ro = LocalEditReadOnly.current
        Row(Modifier.fillMaxWidth().clickable(enabled = !ro) { onPick(key) }.hand(), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(key == selected, { onPick(key) }, enabled = !ro)
            Column(Modifier.padding(start = 4.dp)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                if (desc != null) Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
