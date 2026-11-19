package app.meows.vpn;

import app.meows.vpn.i18n.Lang;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile: one tap to connect / disconnect. */
public class XKTileService extends TileService {
    static void refresh(Context c) {
        if (c == null || Build.VERSION.SDK_INT < 24) return;
        try { TileService.requestListeningState(c, new ComponentName(c, XKTileService.class)); } catch (Exception ignored) { }
    }

    @Override public void onStartListening() { Lang.init(this); update(); }

    private void update() {
        Tile t = getQsTile();
        if (t == null) return;
        int s = XKVpnService.state;
        t.setState(s == XKVpnService.CONNECTED ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel("Meows");
        if (Build.VERSION.SDK_INT >= 29) t.setSubtitle(s == XKVpnService.CONNECTED ? Lang.tr("Подключено") : s == XKVpnService.CONNECTING ? Lang.tr("Подключение…") : Lang.tr("Выключен"));
        t.updateTile();
    }

    @Override public void onClick() {
        XKAppHolder.ctx = getApplicationContext();
        if (XKVpnService.state != XKVpnService.IDLE) {
            XKVpnService.stop(this);
        } else if (VpnService.prepare(this) != null || !XKStore.get(this).hasSubscription()) {
            Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(i);
        } else {
            XKVpnService.start(this);
        }
        update();
    }
}
