package app.meows.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class XKVpnService extends VpnService {
    public static final String ACTION_START = "app.meows.vpn.START";
    public static final String ACTION_STOP = "app.meows.vpn.STOP";
    public static final String ACTION_PAUSE = "app.meows.vpn.PAUSE";

    public static final int IDLE = 0, CONNECTING = 1, CONNECTED = 2;
    public static volatile int state = IDLE;
    public static volatile long connectedAt = 0;
    public static volatile String lastError = null;

    public interface Listener { void onState(int state); }
    private static final List<Listener> listeners = new ArrayList<>();
    public static void addListener(Listener l) { synchronized (listeners) { listeners.add(l); } }
    public static void removeListener(Listener l) { synchronized (listeners) { listeners.remove(l); } }

    private static final Handler main = new Handler(Looper.getMainLooper());
    private ParcelFileDescriptor tun;
    private Process proc;
    private volatile boolean stopping;
    private volatile boolean paused;   // туннель снят, уведомление осталось: «Продолжить» / «Отключить»

    /** Apps that must never go through the VPN. */
    static final String[] ALWAYS_DIRECT = {};
    /** Popular Russian apps sent directly when "RU direct" is on. */
    static final String[] RU_APPS = {
            "ru.sberbankmobile", "ru.sberbank.sberbankid", "com.idamob.tinkoff.android", "ru.tinkoff.investing",
            "ru.vtb24.mobilebanking.android", "ru.alfabank.mobile.android", "ru.raiffeisennews", "ru.gazprombank.android.mobilebank.app",
            "ru.rostel", "ru.gosuslugi.goskey", "ru.fns.lkfl", "ru.mos.app", "ru.mos.polls",
            "ru.yandex.searchplugin", "com.yandex.browser", "ru.yandex.taxi", "ru.yandex.yandexmaps", "ru.yandex.music",
            "ru.yandex.market", "com.yandex.lavka", "ru.yandex.eda", "ru.yandex.disk", "ru.yandex.mail", "ru.yandex.yandexnavi",
            "ru.kinopoisk", "com.yandex.bank", "com.yandex.go", "ru.beru.android",
            "ru.ozon.app.android", "com.wildberries.ru", "com.avito.android", "ru.hh.android", "ru.cian.main",
            "com.vkontakte.android", "com.vk.im", "ru.mail.mailapp", "ru.ok.android", "ru.rutube.app", "ru.vk.store",
            "ru.mts.mymts", "ru.megafon.mlk", "ru.beeline.services", "ru.tele2.mytele2", "ru.yota.android",
            "ru.dodopizza.app", "ru.perekrestok.app", "ru.pyaterochka.app", "ru.magnit", "ru.lenta.lentochka", "ru.auchan.mobile",
            "ru.rzd.pass", "ru.aeroflot", "ru.tutu.tutu_emp", "ru.dublgis.dgismobile", "ru.sbermegamarket.app", "ru.samokat.app",
            "ru.kuper.app", "ru.sbcs.store", "ru.pochta", "ru.cdek.app", "ru.okko.tv", "ru.ivi.client", "ru.more.play", "ru.start.androidmobile",
            "ru.max.messenger", "ru.oneme.app"
    };

    static void broadcast(final int s) {
        state = s;
        main.post(new Runnable() { public void run() {
            List<Listener> copy;
            synchronized (listeners) { copy = new ArrayList<>(listeners); }
            for (Listener l : copy) l.onState(s);
            XKTileService.refresh(XKAppHolder.ctx);
        }});
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        XKAppHolder.ctx = getApplicationContext();
        String a = intent != null ? intent.getAction() : ACTION_START; // null = always-on restart
        if (ACTION_STOP.equals(a)) { stopVpn(null); return START_NOT_STICKY; }
        if (ACTION_PAUSE.equals(a)) { pauseVpn(); return START_STICKY; }
        if (state != IDLE) return START_STICKY;
        paused = false;
        Notification n0 = notification("Подключаемся…");
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n0, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n0);
        broadcast(CONNECTING);
        lastError = null;
        new Thread(new Runnable() { public void run() { startVpn(); } }, "xk-start").start();
        return START_STICKY;
    }

    @Override public void onRevoke() { stopVpn(null); }
    @Override public void onDestroy() { stopVpn(null); super.onDestroy(); }

    private XKStore st0() { return XKStore.get(this); }

    private Notification notification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel("vpn") == null) {
            NotificationChannel ch = new NotificationChannel("vpn", "VPN-подключение", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        int fl = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), fl);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, XKVpnService.class).setAction(ACTION_STOP), fl);
        PendingIntent pause = PendingIntent.getService(this, 2, new Intent(this, XKVpnService.class).setAction(ACTION_PAUSE), fl);
        PendingIntent resume = PendingIntent.getService(this, 3, new Intent(this, XKVpnService.class).setAction(ACTION_START), fl);
        // название приложения система показывает мелко сверху; заголовок — сервер, под ним — состояние
        String title = "Meows";
        try { List<XKStore.Server> l = st0().servers(); int ix = st0().index(); if (ix < l.size()) title = l.get(ix).title; } catch (Exception ignored) { }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "vpn") : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
                .setContentIntent(open).setOngoing(true).setColor(0xFF7CD62B);
        if (paused) b.addAction(new Notification.Action.Builder(null, "Продолжить", resume).build());
        else if (state == CONNECTED) b.addAction(new Notification.Action.Builder(null, "Пауза", pause).build());
        b.addAction(new Notification.Action.Builder(null, "Отключить", stop).build());
        return b.build();
    }

    // IPv4 routes covering everything except private/LAN ranges (works on every Android version)
    private static final long[][] LAN = {{0x0A000000L, 8}, {0xAC100000L, 12}, {0xC0A80000L, 16}, {0xA9FE0000L, 16}, {0xE0000000L, 4}};
    private static final long[] KEEP = {0xAC130000L, 30}; // our own TUN/DNS 172.19.0.0/30 must stay inside
    private static boolean inside(long a, int len, long[] r) { // [a/len] fully inside r
        if (len < r[1]) return false;
        long m = r[1] == 0 ? 0 : (0xFFFFFFFFL << (32 - r[1])) & 0xFFFFFFFFL;
        return (a & m) == r[0];
    }
    private static boolean overlaps(long a, int len, long[] r) {
        int l = (int) Math.min(len, r[1]);
        long m = l == 0 ? 0 : (0xFFFFFFFFL << (32 - l)) & 0xFFFFFFFFL;
        return (a & m) == (r[0] & m);
    }
    private static String ip4(long a) { return ((a >> 24) & 255) + "." + ((a >> 16) & 255) + "." + ((a >> 8) & 255) + "." + (a & 255); }
    private static void addRoutesExceptLan(Builder b, long a, int len) {
        boolean keep = overlaps(a, len, KEEP);
        if (inside(a, len, KEEP)) { b.addRoute(ip4(a), len); return; }
        boolean any = false;
        for (long[] r : LAN) {
            if (inside(a, len, r) && !keep) return;
            if (overlaps(a, len, r)) any = true;
        }
        if (!any && !keep) { b.addRoute(ip4(a), len); return; }
        if (len >= 32) { b.addRoute(ip4(a), len); return; }
        addRoutesExceptLan(b, a, len + 1);
        addRoutesExceptLan(b, a | (1L << (31 - len)), len + 1);
    }

    private void excludeApps(Builder b, XKStore st) {
        Set<String> pkgs = new LinkedHashSet<>();
        pkgs.add(getPackageName()); // our own traffic (subscription, pings, core) goes direct
        for (String p : ALWAYS_DIRECT) pkgs.add(p);
        PackageManager pm = getPackageManager();
        try {
            for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
                String label = String.valueOf(pm.getApplicationLabel(ai));
                String low = ai.packageName.toLowerCase();
                
            }
        } catch (Exception ignored) { }
        if (st.ruApps()) for (String p : RU_APPS) pkgs.add(p);
        pkgs.addAll(st.excludedApps());
        for (String p : pkgs) {
            try { b.addDisallowedApplication(p); } catch (PackageManager.NameNotFoundException ignored) { }
        }
    }

    private void startVpn() {
        stopping = false;
        XKStore st = XKStore.get(this);
        try {
            if (!st.hasSubscription()) throw new Exception("Нет подписки. Добавьте ссылку в приложении.");
            // geo files
            File assetDir = new File(getFilesDir(), "geo");
            assetDir.mkdirs();
            for (String f : new String[]{"geoip.dat", "geosite.dat"}) {
                File out = new File(assetDir, f);
                long want = -1;
                try { android.content.res.AssetFileDescriptor afd = getAssets().openFd(f); want = afd.getLength(); afd.close(); } catch (Exception ignored) { }
                // geo-файлы, скачанные по ссылкам из панели (маркер custom), не перезаписываем вшитыми
                boolean custom = new File(assetDir, "custom").exists();
                if (!out.exists() || (!custom && want >= 0 && out.length() != want)) copyAsset(f, out);
            }
            // config for the core
            org.json.JSONObject cfg = new org.json.JSONObject();
            cfg.put("sub", st.body()); cfg.put("index", st.index()); cfg.put("opts", st.optsJSON());
            File cf = new File(getFilesDir(), "core.json");
            FileOutputStream fo = new FileOutputStream(cf);
            fo.write(cfg.toString().getBytes("UTF-8")); fo.close();

            Builder b = new Builder();
            b.setSession("Meows").setMtu(1500)
                    .addAddress("172.19.0.1", 30)
                    .addAddress("fd66:f83a:c650::1", 126).addRoute("::", 0)
                    .addDnsServer("172.19.0.2");
            if (st.flag(XKStore.P_LAN, true)) addRoutesExceptLan(b, 0L, 0); else b.addRoute("0.0.0.0", 0);
            if (Build.VERSION.SDK_INT >= 29) b.setMetered(false);
            excludeApps(b, st);
            int fl = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
            b.setConfigureIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), fl));
            tun = b.establish();
            if (tun == null) throw new Exception("Android не дал создать VPN. Разрешите подключение.");

            String sock = "meows_tun_" + android.os.Process.myPid();
            final LocalServerSocket server = new LocalServerSocket(sock);
            String exe = new File(getApplicationInfo().nativeLibraryDir, "libxkcore.so").getAbsolutePath();
            ProcessBuilder pb = new ProcessBuilder(exe, cf.getAbsolutePath(), assetDir.getAbsolutePath(), sock);
            pb.redirectErrorStream(true);
            proc = pb.start();

            // hand the TUN fd to the core
            final FileDescriptor fd = tun.getFileDescriptor();
            Thread sender = new Thread(new Runnable() { public void run() {
                try {
                    LocalSocket s = server.accept();
                    s.setFileDescriptorsForSend(new FileDescriptor[]{fd});
                    OutputStream os = s.getOutputStream();
                    os.write(1); os.flush();
                    Thread.sleep(300);
                    s.close();
                } catch (Exception ignored) { }
                try { server.close(); } catch (Exception ignored) { }
            }});
            sender.start();

            // read core output: XKOK / XKERR ...
            BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
            String line; boolean ok = false; StringBuilder err = new StringBuilder();
            while ((line = r.readLine()) != null) {
                if (line.startsWith("XKOK")) { ok = true; break; }
                if (line.startsWith("XKERR")) err.append(line.substring(5).trim());
            }
            if (!ok) throw new Exception(err.length() > 0 ? err.toString() : "Ядро VPN не запустилось");
            connectedAt = System.currentTimeMillis();
            broadcast(CONNECTED);
            ((android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1, notification("Подключено"));
            // keep draining output; if the core dies, stop
            final BufferedReader fr = r;
            new Thread(new Runnable() { public void run() {
                try { while (fr.readLine() != null) { } } catch (Exception ignored) { }
                if (!stopping) stopVpn("Ядро VPN остановилось");
            }}).start();
        } catch (Exception e) {
            stopVpn(e.getMessage());
        }
    }

    private void copyAsset(String name, File out) throws Exception {
        InputStream in = getAssets().open(name);
        FileOutputStream fo = new FileOutputStream(out);
        byte[] buf = new byte[65536]; int n;
        while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
        fo.close(); in.close();
    }

    /** Пауза: снимаем туннель и ядро, но сервис и уведомление остаются — можно продолжить одной кнопкой. */
    private synchronized void pauseVpn() {
        if (state != CONNECTED) return;
        paused = true; stopping = true;
        if (proc != null) { try { proc.destroy(); } catch (Exception ignored) { } proc = null; }
        if (tun != null) { try { tun.close(); } catch (Exception ignored) { } tun = null; }
        connectedAt = 0;
        broadcast(IDLE);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1, notification("Приостановлено"));
    }

    private synchronized void stopVpn(String error) {
        if (stopping && state == IDLE && !paused) return;
        paused = false;
        stopping = true;
        if (error != null) lastError = error;
        if (proc != null) { try { proc.destroy(); } catch (Exception ignored) { } proc = null; }
        if (tun != null) { try { tun.close(); } catch (Exception ignored) { } tun = null; }
        connectedAt = 0;
        broadcast(IDLE);
        try { stopForeground(true); } catch (Exception ignored) { }
        stopSelf();
    }

    public static void start(Context c) {
        Intent i = new Intent(c, XKVpnService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    public static void stop(Context c) {
        c.startService(new Intent(c, XKVpnService.class).setAction(ACTION_STOP));
    }
}
