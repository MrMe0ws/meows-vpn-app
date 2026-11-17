package app.meows.desktop.ui

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
import app.meows.desktop.AppModel
import app.meows.desktop.ui.home.HomeScreen
import app.meows.desktop.ui.settings.SettingsScreen

/** Корень: Главная ⇄ Настройки, снэкбар и диалог ошибки. */
@Composable
fun MeowsApp(app: AppModel) {
    var settings by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(app.toast) { app.toast?.let { snackbar.showSnackbar(it); app.toast = null } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        if (settings) SettingsScreen(app) { settings = false } else HomeScreen(app) { settings = true }
    }
    UpdateDialog(app)
    app.error?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { app.error = null },
            title = { Text(title) }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { app.error = null }) { Text("Понятно") } },
        )
    }
}