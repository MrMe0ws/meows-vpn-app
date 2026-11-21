package app.meows.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.meows.desktop.ui.components.hand

/** Строка настроек с переходом на подстраницу (стрелка справа). */
@Composable
internal fun NavRow(title: String, sub: String?, onClick: () -> Unit) = ListItem(
    modifier = Modifier.clickable(onClick = onClick).hand(), colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    headlineContent = { Text(title) }, supportingContent = sub?.let { { Text(it) } },
    trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
)
