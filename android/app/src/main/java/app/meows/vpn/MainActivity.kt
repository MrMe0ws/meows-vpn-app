package app.meows.vpn

import app.meows.vpn.i18n.Lang
import app.meows.vpn.i18n.tr
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
        else vm.alert(tr("VPN не разрешён"), tr("Разрешите Meows создать VPN-подключение."))
    }
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val qrScanner = registerForActivityResult(ScanContract()) { r ->
        val text = r.contents?.trim().orEmpty()
        if (text.isNotEmpty()) vm.addSubscription(text)
    }

    override fun onStop() {
        super.onStop()
        AppIcon.applyPending(this)   // смена иконки вступает в силу, когда приложение свёрнуто
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppIcon.ensureOne(this)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        Lang.init(this)
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
        vm.refreshStale()
    }

    /** meows://add/<ссылка подписки> (и happ://add/…). Ссылка может прийти URL-кодированной. */
    private fun handleIntent(i: Intent?) {
        val s = i?.data?.toString() ?: return
        if (!s.startsWith("meows:", true) && !s.startsWith("happ:", true)) return
        vm.addSubscription(s)   // нормализация ссылки — в XKStore.fetch (LinkNormalizer)
    }

    private fun toggle() {
        if (vm.vpnState != XKVpnService.IDLE) { vm.stopVpn(); return }
        if (!vm.st.hasSubscription()) { vm.alert(tr("Сначала подписка"), tr("Добавьте ссылку подписки, чтобы подключиться.")); return }
        val prep = VpnService.prepare(this)
        if (prep != null) vpnPermission.launch(prep) else vm.startVpn()
    }

    private fun scanQr() {
        qrScanner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(tr("Наведите камеру на QR-код подписки"))
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }

    companion object { private var autoConnectDone = false }
}
