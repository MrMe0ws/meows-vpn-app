package app.meows.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel
import app.meows.desktop.ui.home.AddDialog
import app.meows.desktop.ui.home.HomeScreen
import app.meows.desktop.ui.about.AboutScreen
import app.meows.desktop.ui.logs.LogsScreen
import app.meows.desktop.ui.nav.MeowsRail
import app.meows.desktop.ui.nav.Page
import app.meows.desktop.ui.settings.SettingsScreen

/** Корень: слева рейка разделов, справа — выбранный раздел; снэкбар, диалог добавления подписки и ошибки. */
@Composable
fun MeowsApp(app: AppModel) {
    var page by remember { mutableStateOf(Page.Home) }
    var adding by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(app.toast) { app.toast?.let { snackbar.showSnackbar(it); app.toast = null } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Row(Modifier.fillMaxSize().padding(pad)) {
            MeowsRail(page, { page = it }, onAdd = { adding = true })
            when (page) {
                Page.Home -> HomeScreen(app, onAdd = { adding = true })
                Page.Settings -> Centered { SettingsScreen(app) { page = Page.Home } }
                Page.Logs -> Centered { LogsScreen() }
                Page.About -> Centered { AboutScreen(app) }
            }
        }
    }
    if (adding) AddDialog(onAdd = { app.addSubscription(it); page = Page.Home }, onDismiss = { adding = false })
    UpdateDialog(app)
    app.error?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { app.error = null },
            title = { Text(title) }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { app.error = null }) { Text("Понятно") } },
        )
    }
}

/** Страницы-списки не растягиваем на всю ширину окна: колонка до 760 dp по центру. */
@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) { Box(Modifier.widthIn(max = 760.dp).fillMaxSize()) { content() } }
}
