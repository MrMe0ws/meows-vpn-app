package app.meows.vpn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.meows.vpn.ui.theme.MeowsTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/** Единственная Activity: разрешения, VPN-диалог, QR-сканер, ссылки meows://add/… — всё остальное в Compose (MeowsApp). */
class MainActivity : ComponentActivity() {
    private val vm: MeowsViewModel by viewModels()

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) vm.startVpn()
        else vm.alert("VPN не разрешён", "Разрешите Meows создать VPN-подключение.")
    }
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val qrScanner = registerForActivityResult(ScanContract()) { r ->
        val text = r.contents?.trim().orEmpty()
        if (text.isNotEmpty()) vm.addSubscription(text)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            MeowsTheme(vm.themeMode, vm.palette, vm.dynamicColors, vm.amoled) {
                MeowsApp(vm, onToggle = ::toggle, onScanQr = ::scanQr)
            }
        }

        handleIntent(intent)
        if (!autoConnectDone) {
            autoConnectDone = true
            if (vm.st.flag(XKStore.P_AUTOCONN, false) && vm.st.hasSubscription() &&
                XKVpnService.state == XKVpnService.IDLE && VpnService.prepare(this) == null
            ) vm.startVpn()
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }

    override fun onResume() {
        super.onResume()
        if (vm.st.needsAutoUpdate()) vm.refresh(true)
    }

    /** meows://add/<ссылка подписки> (и happ://add/…). Ссылка может прийти URL-кодированной. */
    private fun handleIntent(i: Intent?) {
        val s = i?.data?.toString() ?: return
        val k = s.indexOf("://add/")
        if (k < 0) return
        var link = s.substring(k + 7)
        if (link.startsWith("http%3A", true) || link.startsWith("https%3A", true)) link = Uri.decode(link)
        vm.addSubscription(link)
    }

    private fun toggle() {
        if (vm.vpnState != XKVpnService.IDLE) { vm.stopVpn(); return }
        if (!vm.st.hasSubscription()) { vm.alert("Сначала подписка", "Добавьте ссылку подписки, чтобы подключиться."); return }
        val prep = VpnService.prepare(this)
        if (prep != null) vpnPermission.launch(prep) else vm.startVpn()
    }

    private fun scanQr() {
        qrScanner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Наведите камеру на QR-код подписки")
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }

    companion object { private var autoConnectDone = false }
}
