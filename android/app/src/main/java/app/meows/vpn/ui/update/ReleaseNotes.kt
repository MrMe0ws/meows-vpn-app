package app.meows.vpn.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Описание релиза (markdown с GitHub) без сырой разметки: заголовки, списки, **жирный**, `код`, [ссылки](…) — только текстом. */
@Composable
fun ReleaseNotesText(notes: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        notes.trim().lines().forEachIndexed { index, raw ->
            val line = raw.trimEnd()
            val heading = HEADING.matchEntire(line)
            val bullet = BULLET.matchEntire(line)
            when {
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                heading != null -> Text(
                    inline(heading.groupValues[1]), Modifier.padding(top = if (index == 0) 0.dp else 8.dp),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                )
                bullet != null -> Row {
                    Text("•", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(inline(bullet.groupValues[1]), style = MaterialTheme.typography.bodyMedium)
                }
                else -> Text(inline(line.removePrefix("> ")), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val HEADING = Regex("""^#{1,6}\s+(.*)$""")
private val BULLET = Regex("""^\s*[-*+]\s+(.*)$""")
private val INLINE = Regex("""\*\*(.+?)\*\*|`(.+?)`|\[(.+?)]\(.+?\)""")

private fun inline(s: String): AnnotatedString = buildAnnotatedString {
    var pos = 0
    for (m in INLINE.findAll(s)) {
        append(s.substring(pos, m.range.first))
        when {
            m.groups[1] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
            m.groups[2] != null -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(m.groupValues[2]) }
            else -> append(m.groupValues[3])
        }
        pos = m.range.last + 1
    }
    append(s.substring(pos))
}
