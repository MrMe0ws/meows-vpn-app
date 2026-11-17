package app.meows.vpn.ui.settings.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.meows.vpn.MeowsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class AppEntry(val pkg: String, val label: String)

/** Приложения, которые целиком работают без VPN. Применяется при выходе со страницы. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsPage(vm: MeowsViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val chosen = remember { mutableStateListOf<String>().apply { addAll(vm.st.excludedApps()) } }
    var query by remember { mutableStateOf("") }
    val apps by produceState(emptyList<AppEntry>()) {
        value = withContext(Dispatchers.Default) {
            val pm = ctx.packageManager
            pm.getInstalledApplications(0)
                .filter { it.packageName != ctx.packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { AppEntry(it.packageName, pm.getApplicationLabel(it).toString()) }
                .sortedBy { it.label.lowercase() }
        }
    }
    val shown = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, true) || it.pkg.contains(query, true) }
    }

    val vmNow by rememberUpdatedState(vm)
    DisposableEffect(Unit) {
        onDispose { if (chosen.toSet() != vmNow.st.excludedApps()) vmNow.setExcludedApps(chosen.toSet()) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Приложения без VPN") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true, placeholder = { Text("Поиск приложения") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
                items(shown, key = { it.pkg }) { a ->
                    val on = a.pkg in chosen
                    val icon by produceState<ImageBitmap?>(null, a.pkg) {
                        value = withContext(Dispatchers.Default) {
                            runCatching { ctx.packageManager.getApplicationIcon(a.pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
                        }
                    }
                    ListItem(
                        modifier = Modifier.clickable { if (on) chosen.remove(a.pkg) else chosen.add(a.pkg) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            val ic = icon
                            if (ic != null) Image(ic, null, Modifier.size(40.dp)) else Box(Modifier.size(40.dp))
                        },
                        headlineContent = { Text(a.label, maxLines = 1) },
                        supportingContent = { Text(a.pkg, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
                        trailingContent = { Checkbox(on, { if (it) chosen.add(a.pkg) else chosen.remove(a.pkg) }) },
                    )
                }
            }
        }
    }
}
