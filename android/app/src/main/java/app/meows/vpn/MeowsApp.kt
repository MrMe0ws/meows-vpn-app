package app.meows.vpn

import app.meows.vpn.i18n.tr
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.meows.vpn.ui.home.HomeScreen
import app.meows.vpn.ui.settings.SettingsScreen
import app.meows.vpn.ui.update.UpdateDialog

/** Корень интерфейса: Главная ⇄ Настройки + общие диалоги и снэкбар. */
@Composable
fun MeowsApp(vm: MeowsViewModel, onToggle: () -> Unit, onScanQr: () -> Unit) {
    var settings by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.toast) {
        vm.toast?.let { snackbar.showSnackbar(it); vm.toast = null }
    }
    // Назад из корня настроек — на главную; внутри разделов «назад» обрабатывает сам SettingsScreen
    BackHandler(enabled = settings) { settings = false }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { _ ->
        AnimatedContent(
            targetState = settings,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screens",
        ) { s ->
            if (s) SettingsScreen(vm, onBack = { settings = false })
            else HomeScreen(vm, onToggle, openSettings = { settings = true }, onScanQr = onScanQr)
        }
    }

    if (!Edition.isPlay) UpdateDialog(vm)

    val title = vm.dialogTitle
    if (title != null) {
        AlertDialog(
            onDismissRequest = vm::dismissAlert,
            title = { Text(title) },
            text = { Text(vm.dialogText.orEmpty()) },
            confirmButton = { TextButton(onClick = vm::dismissAlert) { Text(tr("Понятно")) } },
            dismissButton = vm.dialogActionLabel?.let { label -> { TextButton(onClick = vm::runDialogAction) { Text(label) } } },
        )
    }
}
