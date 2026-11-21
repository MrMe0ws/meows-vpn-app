package app.meows.vpn;

import app.meows.vpn.i18n.Lang;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Subscription storage, parsing, fetching, ping, settings. */
public final class XKStore {
    public static final String P_THEME = "theme";          // 0 auto, 1 light, 2 dark
    public static final String P_RU = "ruDirect";          // default false (расширенное правило; основной split — в шаблоне подписки)
    public static final String P_ADS = "blockAds";
    public static final String P_ALL_VPN = "allVpn";       // default false: весь трафик через VPN, кроме торрентов и локальной сети
    public static final String P_ROUTE_PROFILE = "routeProfile"; // "sub" — правила подписки (по умолчанию), "meows" — встроенный профиль
    public static final String P_AUTO = "autoMode";       // 0 by sub, -1 off, N hours
    public static final String P_LAUNCH = "updateOnLaunch";// default true
    public static final String P_REMIND = "remind";        // default true
    public static final String P_AUTOSWITCH = "autoswitch"; // default true: сменить сервер, если текущий перестал отвечать
    public static final String P_EXCLUDED = "excludedApps";
    public static final String P_CATS = "bypassCats";
    public static final String P_SITES = "bypassSites";
    public static final String P_PING_MODE = "pingMode";      // 0 real (through the server), 1 TCP
    public static final String P_PING_URL = "pingUrl";        // 0 Google, 1 Cloudflare, 2 Apple
    public static final String P_PING_TO = "pingTimeout";     // seconds, default PING_TO_DEFAULT
    public static final int PING_TO_DEFAULT = 6;              // 3 s was too short on slow roaming / weak LTE: every server showed "no answer"
    public static final String P_PING_LAUNCH = "pingOnLaunch";// default false
    public static final String P_PING_KEEP = "pingKeep";      // seconds to show results, 0 = always; default 60
    public static final String P_AUTOCONN = "autoConnect";    // default false
    public static final String P_LAN = "allowLan";            // default true
    public static final String P_RU_APPS = "ruApps";          // default true
    public static String LOCK_HOST = "";                      // white-label lock to one subscription host
    public static final String[] PING_URLS = {"https://www.gstatic.com/generate_204", "https://cp.cloudflare.com/generate_204", "https://captive.apple.com/hotspot-detect.html"};
    public int pref(String k, int def) { return sp.getInt(k, def); }
    public boolean flag(String k, boolean def) { return sp.getBoolean(k, def); }

    /** id, title, hint */
    public static final String[][] CATEGORIES = {
            {"ru", "Российские сайты и сервисы", "Банки, Госуслуги, магазины, Яндекс, VK — списки roscomvpn"},
            {"apple", "Apple", "iCloud, App Store, обновления"},
            {"microsoft", "Microsoft", "Outlook, Teams, OneDrive"},
            {"games", "Игры", "Steam, Epic Games, Riot, Faceit"}};
    public Set<String> bypassCats() { return new HashSet<>(sp.getStringSet(P_CATS, new HashSet<String>())); }
    public void setBypassCats(Set<String> s) { sp.edit().putStringSet(P_CATS, s).apply(); }
    public List<String> bypassSites() {
        List<String> l = new ArrayList<>(sp.getStringSet(P_SITES, new HashSet<String>()));
        java.util.Collections.sort(l);
        return l;
    }
    public void setBypassSites(java.util.Collection<String> s) { sp.edit().putStringSet(P_SITES, new HashSet<>(s)).apply(); }

    public static String normalizeSite(String raw) {
        String d = raw == null ? "" : raw.trim().toLowerCase();
        for (String p : new String[]{"https://", "http://", "*."}) if (d.startsWith(p)) d = d.substring(p.length());
        if (d.startsWith("www.")) d = d.substring(4);
        for (char ch : new char[]{'/', '?', '#', ':'}) { int i = d.indexOf(ch); if (i >= 0) d = d.substring(0, i); }
        return d.contains(".") ? d : "";
    }

    public static final class Server {
        public String name, flag, title, subtitle;
        public List<String[]> endpoints = new ArrayList<>(); // host, port, udp(0/1)
        public int proxyCount;
    }

    public interface Callback { void done(String error); }

    /** Несколько подписок: слот 0 хранит данные под прежними ключами «sub.*» (обновление приложения ничего не теряет), остальные — «sub<N>.*». */
    private static final Map<Integer, XKStore> insts = new HashMap<>();
    private final int slot;
    private final Context ctx;
    private final SharedPreferences sp;
    private List<Server> servers;
    public final Map<Integer, Integer> pings = new HashMap<>(); // ms, -1 fail, -2 udp
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(8);

    /** Активная подписка — та, с которой подключается VPN. Всё приложение, кроме списка подписок, работает с ней. */
    public static synchronized XKStore get(Context c) { return byId(c, activeId(c)); }

    public static synchronized XKStore byId(Context c, int id) {
        XKStore s = insts.get(id);
        if (s == null) { s = new XKStore(c.getApplicationContext(), id); insts.put(id, s); }
        return s;
    }

    private static SharedPreferences gp(Context c) { return c.getApplicationContext().getSharedPreferences("xk", Context.MODE_PRIVATE); }

    public static synchronized List<Integer> slotIds(Context c) {
        List<Integer> r = new ArrayList<>();
        for (String x : gp(c).getString("sub.slots", "0").split(",")) { try { r.add(Integer.parseInt(x.trim())); } catch (Exception ignored) { } }
        if (r.isEmpty()) r.add(0);
        return r;
    }

    private static void saveSlots(Context c, List<Integer> ids) {
        StringBuilder b = new StringBuilder();
        for (int i : ids) { if (b.length() > 0) b.append(','); b.append(i); }
        gp(c).edit().putString("sub.slots", b.toString()).apply();
    }

    public static synchronized List<XKStore> all(Context c) {
        List<XKStore> r = new ArrayList<>();
        for (int id : slotIds(c)) r.add(byId(c, id));
        return r;
    }

    public static synchronized int activeId(Context c) {
        List<Integer> ids = slotIds(c);
        int a = gp(c).getInt("sub.active", ids.get(0));
        return ids.contains(a) ? a : ids.get(0);
    }

    public static synchronized void activate(Context c, int id) { if (slotIds(c).contains(id)) gp(c).edit().putInt("sub.active", id).apply(); }

    /** Куда сохранить ссылку: подписка с такой же ссылкой (обновим её), иначе пустой слот, иначе новый. */
    public static synchronized XKStore slotFor(Context c, String link) {
        List<XKStore> all = all(c);
        for (XKStore s : all) if (s.url().equals(link)) return s;
        for (XKStore s : all) if (s.url().isEmpty()) return s;
        List<Integer> ids = slotIds(c);
        int next = 0; for (int i : ids) next = Math.max(next, i + 1);
        ids.add(next); saveSlots(c, ids);
        return byId(c, next);
    }

    /** Удалить подписку; последнюю не убираем из списка слотов, а очищаем. */
    public static synchronized void removeSlot(Context c, int id) {
        XKStore s = byId(c, id);
        s.clear();
        List<Integer> ids = slotIds(c);
        if (ids.size() > 1) { ids.remove((Integer) id); saveSlots(c, ids); insts.remove(id); }
    }

    private XKStore(Context c, int slot) {
        ctx = c;
        this.slot = slot;
        sp = c.getSharedPreferences("xk", Context.MODE_PRIVATE);
    }

    public int id() { return slot; }

    /** Закреплённые подписки показываются на главной выше остальных. */
    public boolean isPinned() { return sp.getBoolean(k("pinned"), false); }
    public void setPinned(boolean v) { sp.edit().putBoolean(k("pinned"), v).apply(); }
    private String k(String n) { return slot == 0 ? "sub." + n : "sub" + slot + "." + n; }

    public SharedPreferences prefs() { return sp; }
    public String url() { return sp.getString(k("url"), ""); }
    public String body() { return sp.getString(k("body"), ""); }
    public boolean hasSubscription() { return body().length() > 0; }
    public long updated() { return sp.getLong(k("updated"), 0); }

    public int index() {
        int i = sp.getInt(k("index"), 0);
        return (i >= 0 && i < servers().size()) ? i : 0;
    }
    public void setIndex(int i) { sp.edit().putInt(k("index"), i).apply(); }

    /** Расширенно: все российские сайты/зоны напрямую поверх правил подписки. По умолчанию выключено. */
    public boolean ruDirect() { return sp.getBoolean(P_RU, false); }
    /** Российские приложения (банки, Госуслуги и т.д.) целиком мимо VPN. По умолчанию включено. */
    public boolean ruApps() { return sp.getBoolean(P_RU_APPS, true); }
    public boolean allVpn() { return sp.getBoolean(P_ALL_VPN, false); }
    public boolean blockAds() { return sp.getBoolean(P_ADS, false); }
    public boolean remind() { return sp.getBoolean(P_REMIND, true); }
    public boolean updateOnLaunch() { return sp.getBoolean(P_LAUNCH, true); }

    public Set<String> excludedApps() { return new HashSet<>(sp.getStringSet(P_EXCLUDED, new HashSet<String>())); }
    public void setExcludedApps(Set<String> s) { sp.edit().putStringSet(P_EXCLUDED, s).apply(); }

    // ---------- headers ----------
    private JSONObject headers() {
        try { return new JSONObject(sp.getString(k("headers"), "{}")); } catch (Exception e) { return new JSONObject(); }
    }
    private String header(String name) {
        String v = headers().optString(name, null);
        return v == null || v.length() == 0 ? null : v;
    }
    private static String b64prefixed(String v) {
        if (v == null) return null;
        if (v.startsWith("base64:")) {
            try { return new String(Base64.decode(v.substring(7), Base64.DEFAULT), "UTF-8"); } catch (Exception e) { return null; }
        }
        return v;
    }
    public String title() { String t = b64prefixed(header("profile-title")); return t != null ? t : "Meows"; }
    public String announce() { return b64prefixed(header("announce")); }
    public String supportUrl() { return header("support-url"); }
    public String webUrl() { return header("profile-web-page-url"); }
    public int headerInterval() { try { return Integer.parseInt(header("profile-update-interval").trim()); } catch (Exception e) { return 0; } }

    private long userinfo(String key) {
        String ui = header("subscription-userinfo");
        if (ui == null) return 0;
        for (String part : ui.split(";")) {
            String[] kv = part.trim().split("=");
            if (kv.length == 2 && kv[0].trim().equals(key)) {
                try { return Long.parseLong(kv[1].trim()); } catch (Exception ignored) { }
            }
        }
        return 0;
    }
    public long used() { return userinfo("upload") + userinfo("download"); }
    public long total() { return userinfo("total"); }
    public long expire() { return userinfo("expire"); }

    public int autoUpdateHours() {
        int m = sp.getInt(P_AUTO, 0);
        if (m < 0) return 0;
        if (m == 0) return headerInterval() > 0 ? headerInterval() : 12;
        return m;
    }
    public boolean needsAutoUpdate() {
        if (url().length() == 0) return false;
        int h = autoUpdateHours();
        return h > 0 && (System.currentTimeMillis() - updated()) > h * 3600_000L;
    }

    public String optsJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("ruDirect", ruDirect());
            o.put("blockAds", blockAds());
            o.put("allVpn", allVpn());
            o.put("noVV", true);
            o.put("profile", app.meows.vpn.routing.RoutingProfiles.INSTANCE.selectedJson(this));
            o.put("cats", new JSONArray(bypassCats()));
            o.put("domains", new JSONArray(bypassSites()));
            return o.toString();
        } catch (Exception e) {
            return "{\"ruDirect\":" + ruDirect() + "}";
        }
    }

    // ---------- parsing ----------
    private JSONArray configs() {
        String b = body().trim();
        if (b.length() == 0) return new JSONArray();
        if (b.charAt(0) != '[' && b.charAt(0) != '{') {
            try { b = new String(Base64.decode(b, Base64.DEFAULT), "UTF-8").trim(); } catch (Exception e) { return new JSONArray(); }
        }
        try {
            if (b.startsWith("[")) return new JSONArray(b);
            JSONArray a = new JSONArray(); a.put(new JSONObject(b)); return a;
        } catch (Exception e) { return new JSONArray(); }
    }

    private static boolean isProxy(JSONObject ob) {
        String p = ob.optString("protocol");
        return p.length() > 0 && !p.equals("freedom") && !p.equals("blackhole") && !p.equals("dns") && !p.equals("loopback");
    }

    private static String[] endpoint(JSONObject ob) {
        JSONObject s = ob.optJSONObject("settings");
        if (s == null) return null;
        String host = s.optString("address", null); int port = s.optInt("port", 0);
        JSONArray v = s.optJSONArray("vnext"); if (v == null) v = s.optJSONArray("servers");
        if ((host == null || host.length() == 0) && v != null && v.length() > 0) {
            JSONObject f = v.optJSONObject(0);
            if (f != null) { host = f.optString("address", null); port = f.optInt("port", 0); }
        }
        JSONArray peers = s.optJSONArray("peers");
        if ((host == null || host.length() == 0) && peers != null && peers.length() > 0) {
            String ep = peers.optJSONObject(0) != null ? peers.optJSONObject(0).optString("endpoint", "") : "";
            int i = ep.lastIndexOf(':');
            if (i > 0) { host = ep.substring(0, i); try { port = Integer.parseInt(ep.substring(i + 1)); } catch (Exception ignored) { } }
        }
        if (host == null || host.length() == 0) return null;
        host = host.replace("[", "").replace("]", "");
        String proto = ob.optString("protocol");
        JSONObject ss = ob.optJSONObject("streamSettings");
        String net = ss != null ? ss.optString("network") : "";
        boolean udp = proto.equals("hysteria") || proto.equals("wireguard") || net.equals("kcp");
        return new String[]{host, String.valueOf(port == 0 ? 443 : port), udp ? "1" : "0"};
    }

    private static String protoLabel(JSONObject ob) {
        Map<String, String> names = new HashMap<>();
        names.put("vless", "VLESS"); names.put("vmess", "VMess"); names.put("trojan", "Trojan");
        names.put("shadowsocks", "Shadowsocks"); names.put("hysteria", "Hysteria2"); names.put("wireguard", "WireGuard");
        names.put("socks", "SOCKS"); names.put("http", "HTTP");
        String raw = ob.optString("protocol");
        String p = names.containsKey(raw) ? names.get(raw) : raw.toUpperCase();
        StringBuilder sb = new StringBuilder(p);
        JSONObject ss = ob.optJSONObject("streamSettings");
        String sec = ss != null ? ss.optString("security") : "";
        if (sec.equals("reality")) sb.append(" · Reality"); else if (sec.equals("tls")) sb.append(" · TLS");
        Map<String, String> nets = new HashMap<>();
        nets.put("tcp", "TCP"); nets.put("raw", "TCP"); nets.put("xhttp", "XHTTP"); nets.put("ws", "WS"); nets.put("grpc", "gRPC");
        nets.put("httpupgrade", "HTTPUpgrade"); nets.put("kcp", "mKCP"); nets.put("splithttp", "XHTTP");
        String net = ss != null ? ss.optString("network") : "";
        if (p.equals("Hysteria2")) sb.append(" · UDP");
        else if (nets.containsKey(net)) sb.append(" · ").append(nets.get(net));
        else if (!p.equals("WireGuard")) sb.append(" · TCP");
        return sb.toString();
    }

    private static String protoLine(JSONObject ob) {
        Map<String, String> names = new HashMap<>();
        names.put("vless", "VLESS"); names.put("vmess", "VMess"); names.put("trojan", "Trojan");
        names.put("shadowsocks", "Shadowsocks"); names.put("hysteria", "Hysteria2"); names.put("hysteria2", "Hysteria2");
        names.put("wireguard", "WireGuard"); names.put("socks", "SOCKS"); names.put("http", "HTTP");
        String raw = ob.optString("protocol");
        String p = names.containsKey(raw) ? names.get(raw) : raw.toUpperCase();
        Map<String, String> nets = new HashMap<>();
        nets.put("tcp", "TCP"); nets.put("raw", "TCP"); nets.put("xhttp", "XHTTP"); nets.put("ws", "WS"); nets.put("grpc", "gRPC");
        nets.put("httpupgrade", "HTTPUpgrade"); nets.put("kcp", "mKCP"); nets.put("splithttp", "XHTTP"); nets.put("quic", "QUIC");
        JSONObject ss = ob.optJSONObject("streamSettings");
        String net = ss != null ? ss.optString("network") : "";
        String sec = ss != null ? ss.optString("security") : "";
        StringBuilder sb = new StringBuilder(p);
        if (p.equals("Hysteria2") || p.equals("WireGuard")) sb.append(" · UDP");
        else sb.append(" · ").append(nets.containsKey(net) ? nets.get(net) : "TCP");
        if (sec.equals("reality")) sb.append(" · Reality");
        else if (sec.equals("tls") || p.equals("Hysteria2")) sb.append(" · TLS");
        sb.append(" · JSON");
        return sb.toString();
    }

    public String prettyConfig(int i) {
        try {
            JSONObject o = configs().optJSONObject(i);
            return o == null ? "{}" : o.toString(2).replace("\\/", "/");
        } catch (Exception e) { return "{}"; }
    }

    private static String plural(int n, String one, String few, String many) {
        int m10 = n % 10, m100 = n % 100;
        if (m10 == 1 && m100 != 11) return one;
        if (m10 >= 2 && m10 <= 4 && (m100 < 10 || m100 >= 20)) return few;
        return many;
    }

    public synchronized List<Server> servers() {
        if (servers != null) return servers;
        List<Server> list = new ArrayList<>();
        JSONArray cfgs = configs();
        for (int i = 0; i < cfgs.length(); i++) {
            JSONObject cfg = cfgs.optJSONObject(i);
            if (cfg == null) continue;
            Server s = new Server();
            String remarks = cfg.optString("remarks", "");
            if (remarks.length() == 0) remarks = Lang.tr("Сервер ") + (i + 1);
            s.name = remarks;
            // flag emoji at the start (after spaces / invisible chars)
            int st0 = 0;
            while (st0 < remarks.length() && " \t\u200B\u200C\u200D\uFE0F\u2060\u00A0".indexOf(remarks.charAt(st0)) >= 0) st0++;
            if (remarks.length() >= st0 + 4 && Character.isHighSurrogate(remarks.charAt(st0))) {
                int cp1 = remarks.codePointAt(st0), cp2 = remarks.codePointAt(st0 + 2);
                if (cp1 >= 0x1F1E6 && cp1 <= 0x1F1FF && cp2 >= 0x1F1E6 && cp2 <= 0x1F1FF) {
                    s.flag = remarks.substring(st0, st0 + 4);
                    s.title = remarks.substring(st0 + 4).trim();
                }
            }
            // пиратский флаг: U+1F3F4 + ZWJ + U+2620 (+ VS16)
            if (s.flag == null && remarks.startsWith("\uD83C\uDFF4\u200D\u2620", st0)) {
                int end = st0 + 4;
                if (end < remarks.length() && remarks.charAt(end) == '\uFE0F') end++;
                s.flag = remarks.substring(st0, end);
                s.title = remarks.substring(end).trim();
            }
            if (s.title == null) s.title = remarks.trim();
            if (s.title == null || s.title.length() == 0) s.title = remarks;
            LinkedHashSet<String> protos = new LinkedHashSet<>();
            String single = null;
            JSONArray outs = cfg.optJSONArray("outbounds");
            if (outs != null) for (int k = 0; k < outs.length(); k++) {
                JSONObject ob = outs.optJSONObject(k);
                if (ob == null || !isProxy(ob)) continue;
                String[] ep = endpoint(ob);
                if (ep != null) s.endpoints.add(ep);
                single = protoLabel(ob);
                protos.add(single.split(" · ")[0]);
            }
            s.proxyCount = s.endpoints.size();
            // protocol line like Happ: "VLESS · TCP · Reality · JSON", "Hysteria2 · UDP · TLS · JSON"
            JSONObject mainOb = null;
            if (outs != null) for (int k = 0; k < outs.length(); k++) {
                JSONObject ob = outs.optJSONObject(k);
                if (ob != null && isProxy(ob)) { mainOb = ob; break; }
            }
            s.subtitle = mainOb != null ? protoLine(mainOb) : "—";
            list.add(s);
        }
        servers = list;
        return list;
    }

    // ---------- device ----------
    public String hwid() {
        String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null || id.length() < 8) {
            id = sp.getString("hwid", null);
            if (id == null) { id = java.util.UUID.randomUUID().toString().replace("-", ""); sp.edit().putString("hwid", id).apply(); }
        }
        return id;
    }

    private String appVersion() {
        try { return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName; } catch (Exception e) { return "0"; }
    }

    /** Заголовок `routing` из ответа панели (happ://routing/add/<base64 JSON>) — там закреплённые ссылки на geo-файлы. */
    public String routingHeader() { return header("routing"); }

    // ---------- network ----------
    public void fetch(final String raw, final Callback cb) {
        final String link = LinkNormalizer.normalize(raw);   // meows://add/…, кавычки, %3A%2F%2F, https// и т. п. → https://…
        if (!LinkNormalizer.isHttpLink(link)) {
            cb.done(Lang.tr("Это не похоже на ссылку подписки."));
            return;
        }
        if (LOCK_HOST.length() > 0) {
            String host = "";
            try { host = android.net.Uri.parse(link).getHost(); } catch (Exception ignored) { }
            if (host == null || !host.equalsIgnoreCase(LOCK_HOST)) { cb.done(Lang.tr("Эта подписка не поддерживается приложением.")); return; }
        }
        pool.execute(new Runnable() { public void run() {
            String err = null;
            // сначала представляемся как Meows (правило в панели Remnawave); если панель его ещё не знает и отдаёт ссылки,
            // повторяем запрос как Happ — тогда подписка работает в любом случае
            String[] uas = {"Meows/" + appVersion(), "Happ/3.9.0"};
            for (int attempt = 0; attempt < uas.length; attempt++) {
            err = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(link).openConnection();
                c.setConnectTimeout(15000); c.setReadTimeout(20000);
                c.setRequestProperty("User-Agent", uas[attempt]);
                c.setRequestProperty("Accept", "*/*");   // иначе Java шлёт text/html и панель отдаёт HTML-страницу
                c.setRequestProperty("x-hwid", hwid());   // HWID отправляем всегда: по нему панель считает лимит устройств
                c.setRequestProperty("x-device-os", "Android");
                c.setRequestProperty("x-ver-os", Build.VERSION.RELEASE);
                c.setRequestProperty("x-device-model", Build.MANUFACTURER + " " + Build.MODEL);
                c.setRequestProperty("x-app-name", "Meows");
                int code = c.getResponseCode();
                if (code >= 400) {
                    err = Lang.tr("Сервер ответил ") + code + ". " + (code == 404 ? Lang.tr("Подписка не найдена — проверьте ссылку.")
                            : Lang.tr("Возможно, достигнут лимит устройств. Удалите старое устройство в боте или кабинете."));
                } else {
                    InputStream in = c.getInputStream();
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    byte[] buf = new byte[16384]; int n;
                    while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                    in.close();
                    String body = bo.toString("UTF-8");
                    String tr = body.trim();
                    if (tr.length() == 0) err = Lang.tr("Подписка пустая. Возможно, превышен лимит устройств.");
                    else {
                        char c0 = tr.charAt(0);
                        if (c0 != '[' && c0 != '{') {
                            String dec = "";
                            try { dec = new String(Base64.decode(tr, Base64.DEFAULT), "UTF-8").trim(); } catch (Exception ignored) { }
                            if (!(dec.startsWith("[") || dec.startsWith("{")))
                                err = "Сервер отдал ссылки вместо Xray JSON. Включите Xray JSON шаблон для клиентов Meows в Remnawave.";
                        }
                    }
                    if (err == null) {
                        JSONObject h = new JSONObject();
                        for (Map.Entry<String, List<String>> e : c.getHeaderFields().entrySet()) {
                            if (e.getKey() == null || e.getValue() == null || e.getValue().isEmpty()) continue;
                            h.put(e.getKey().toLowerCase(), e.getValue().get(0));
                        }
                        sp.edit().putString(k("url"), link).putString(k("body"), body).putString(k("headers"), h.toString())
                                .putLong(k("updated"), System.currentTimeMillis()).apply();
                        synchronized (XKStore.this) { servers = null; }
                        pings.clear();
                    }
                }
                c.disconnect();
            } catch (Exception e) {
                err = Lang.tr("Нет связи с сервером подписки.") + "\n" + e.getMessage();
            }
            if (err != null && err.startsWith("Сервер отдал ссылки") && attempt + 1 < uas.length) continue;
            break;
            }
            final String fe = err;
            main.post(new Runnable() { public void run() {
                if (fe == null) { scheduleReminders(); XKReceiver.checkTraffic(ctx, XKStore.this); }
                cb.done(fe);
            }});
        }});
    }

    public void clear() {
        sp.edit().remove(k("url")).remove(k("body")).remove(k("headers")).remove(k("updated")).remove(k("index")).remove(k("pinned")).apply();
        synchronized (this) { servers = null; }
        pings.clear();
        scheduleReminders();
    }

    // Honest ping: TCP connect time (best of two). A public server that "answers" in <= 4 ms can
    // only be a local answer (another VPN, carrier proxy) -> send a real TLS ClientHello and time
    // the server's first byte. No answer -> -1 ("—"), never a fake 1 ms.
    static int tcpPing(String host, int port) {
        double best = -1;
        try {
            java.net.InetAddress[] addrs = java.net.InetAddress.getAllByName(host);
            for (java.net.InetAddress ad : addrs) {
                boolean priv = ad.isSiteLocalAddress() || ad.isLoopbackAddress() || ad.isLinkLocalAddress() || isCgnat(ad);
                for (int k = 0; k < 2; k++) {
                    double[] r = probe(ad, port, host, false);
                    if (r[0] < 0) break;
                    double v = r[0];
                    if (v < 10 && !priv) { r = probe(ad, port, host, true); v = r[1] >= 8 ? r[1] : -1; }
                    if (v > 0 && (best < 0 || v < best)) best = v;
                }
                if (best > 0) break;
            }
        } catch (Exception e) { return -1; }
        if (best < 0) return -1;
        return Math.max(1, (int) Math.round(best));
    }
    private static boolean isCgnat(java.net.InetAddress a) {
        byte[] b = a.getAddress(); return b.length == 4 && (b[0] & 255) == 100 && (b[1] & 0xC0) == 64;
    }
    private static double[] probe(java.net.InetAddress ad, int port, String host, boolean tls) {
        double[] out = {-1, -1};
        Socket s = new Socket();
        try {
            long t0 = System.nanoTime();
            s.connect(new InetSocketAddress(ad, port), pingTimeoutMs);
            out[0] = (System.nanoTime() - t0) / 1e6;
            if (tls) {
                s.setSoTimeout(2000);
                byte[] hello = clientHello(host);
                long t1 = System.nanoTime();
                java.io.OutputStream os = s.getOutputStream(); os.write(hello); os.flush();
                if (s.getInputStream().read() >= 0) out[1] = (System.nanoTime() - t1) / 1e6;
            }
        } catch (Exception ignored) {
        } finally { try { s.close(); } catch (Exception ignored) { } }
        return out;
    }
    private static byte[] clientHello(String host) {
        java.io.ByteArrayOutputStream h = new java.io.ByteArrayOutputStream();
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        byte[] r32 = new byte[32];
        h.write(3); h.write(3); rnd.nextBytes(r32); h.write(r32, 0, 32);
        h.write(32); rnd.nextBytes(r32); h.write(r32, 0, 32);
        int[] cs = {0x1301, 0x1302, 0x1303, 0xc02b, 0xc02f, 0xc02c, 0xc030, 0xcca9, 0xcca8};
        w16(h, cs.length * 2); for (int c : cs) w16(h, c);
        h.write(1); h.write(0);
        java.io.ByteArrayOutputStream e = new java.io.ByteArrayOutputStream();
        boolean ip = host.matches("[0-9.]+") || host.contains(":");
        if (!ip && host.length() > 0 && host.length() < 200) {
            byte[] hb = host.getBytes(java.nio.charset.Charset.forName("US-ASCII"));
            w16(e, 0); w16(e, hb.length + 5); w16(e, hb.length + 3); e.write(0); w16(e, hb.length); e.write(hb, 0, hb.length);
        }
        int[] fixed = {0x00,0x0a,0x00,0x06,0x00,0x04,0x00,0x1d,0x00,0x17, 0x00,0x0b,0x00,0x02,0x01,0x00,
            0x00,0x0d,0x00,0x12,0x00,0x10,0x04,0x03,0x08,0x04,0x04,0x01,0x05,0x03,0x08,0x05,0x05,0x01,0x08,0x06,0x06,0x01,
            0x00,0x2b,0x00,0x05,0x04,0x03,0x04,0x03,0x03, 0x00,0x2d,0x00,0x02,0x01,0x01,
            0x00,0x10,0x00,0x0e,0x00,0x0c,0x02,'h','2',0x08,'h','t','t','p','/','1','.','1'};
        for (int b : fixed) e.write(b);
        w16(e, 0x0033); w16(e, 38); w16(e, 36); w16(e, 0x001d); w16(e, 32); rnd.nextBytes(r32); e.write(r32, 0, 32);
        byte[] ext = e.toByteArray(); w16(h, ext.length); h.write(ext, 0, ext.length);
        byte[] body = h.toByteArray();
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        o.write(0x16); o.write(3); o.write(1); w16(o, body.length + 4);
        o.write(1); o.write((body.length >> 16) & 255); o.write((body.length >> 8) & 255); o.write(body.length & 255);
        o.write(body, 0, body.length);
        return o.toByteArray();
    }
    private static void w16(java.io.ByteArrayOutputStream o, int v) { o.write((v >> 8) & 255); o.write(v & 255); }

    /** Ping by tap: either a real request through every server (default) or a TCP connect. */
    public void pingAll(final Runnable progress, final Runnable done) { pingSome(null, progress, done); }

    /** Ping only the given server indexes (null = all). Other servers keep their previous result. */
    public void pingSome(final int[] only, final Runnable progress, final Runnable done) {
        if (pref(P_PING_MODE, 0) == 1) tcpPingSome(only, progress, done); else realPingSome(only, progress, done);
    }

    private int[] targets(int[] only, int size) {
        if (only != null) return only;
        int[] all = new int[size];
        for (int i = 0; i < size; i++) all[i] = i;
        return all;
    }

    /** Real delay: the core builds each server's outbound and fetches the test URL through it (like INCY / v2rayN). */
    private volatile Process measureProc;
    private void realPingSome(final int[] only, final Runnable progress, final Runnable done) {
        final List<Server> list = servers();
        if (list.isEmpty()) { done.run(); return; }
        final int[] tg = targets(only, list.size());
        for (int i : tg) pings.remove(i);
        final int to = Math.max(1, pref(P_PING_TO, PING_TO_DEFAULT)) * 1000;
        int ui = pref(P_PING_URL, 0);
        final String url = PING_URLS[ui >= 0 && ui < PING_URLS.length ? ui : 0];
        final String body = body();
        final int n = tg.length;
        pool.execute(new Runnable() { public void run() {
            final java.util.Set<Integer> got = new java.util.HashSet<>();
            try {
                java.io.File f = new java.io.File(ctx.getFilesDir(), "measure.json");
                java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
                fo.write(body.getBytes("UTF-8")); fo.close();
                StringBuilder idx = new StringBuilder();
                for (int k = 0; k < n; k++) { if (k > 0) idx.append(','); idx.append(tg[k]); }
                String exe = new java.io.File(ctx.getApplicationInfo().nativeLibraryDir, "libxkcore.so").getAbsolutePath();
                ProcessBuilder pb = new ProcessBuilder(exe, "measure", f.getAbsolutePath(), url, String.valueOf(to), idx.toString());
                pb.redirectErrorStream(true);
                final Process p = pb.start();
                measureProc = p;
                final long limit = (long) (to * 2 + 2500) * ((n + 3) / 4) + 3000;
                main.postDelayed(new Runnable() { public void run() { try { p.destroy(); } catch (Exception ignored) { } } }, limit);
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), "UTF-8"));
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.startsWith("XKMS ")) continue;
                    String[] a = line.trim().split(" ");
                    if (a.length < 3) continue;
                    final int i = Integer.parseInt(a[1]); final int ms = Integer.parseInt(a[2]);
                    got.add(i);
                    main.post(new Runnable() { public void run() { pings.put(i, ms > 0 ? ms : -1); progress.run(); } });
                }
                try { p.waitFor(); } catch (Exception ignored) { }
            } catch (Exception ignored) { }
            main.post(new Runnable() { public void run() {
                for (int i : tg) if (!got.contains(i)) pings.put(i, -1);
                done.run();
            }});
        }});
    }

    /** App UID is excluded from the VPN, so pings are honest even while connected. */
    static volatile int pingTimeoutMs = PING_TO_DEFAULT * 1000;
    private void tcpPingSome(final int[] only, final Runnable progress, final Runnable done) {
        pingTimeoutMs = Math.max(1, pref(P_PING_TO, PING_TO_DEFAULT)) * 1000;
        final List<Server> list = servers();
        if (list.isEmpty()) { done.run(); return; }
        final int[] tg = targets(only, list.size());
        for (int i : tg) pings.remove(i);
        final AtomicInteger left = new AtomicInteger(tg.length);
        for (final int idx : tg) {
            final Server s = list.get(idx);
            pool.execute(new Runnable() { public void run() {
                int best = -1; boolean tcp = false;
                for (String[] ep : s.endpoints) {
                    if (ep[2].equals("1")) continue;
                    tcp = true;
                    int ms = tcpPing(ep[0], Integer.parseInt(ep[1]));
                    if (ms > 0 && (best < 0 || ms < best)) best = ms;
                }
                if (!tcp && !s.endpoints.isEmpty()) best = -2;
                final int fb = best;
                main.post(new Runnable() { public void run() {
                    // 1–2 ms while connected can only be a local answer: keep the previous value
                    boolean fake = XKVpnService.state == XKVpnService.CONNECTED && fb > 0 && fb <= 3;
                    if (!fake) pings.put(idx, fb);
                    progress.run();
                    if (left.decrementAndGet() == 0) done.run();
                }});
            }});
        }
    }
    // ---------- expiry reminders ----------
    public void scheduleReminders() {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        long expire = expire() * 1000L;
        long[] before = {3 * 86400_000L, 86400_000L, 0};
        for (int k = 0; k < 3; k++) {
            Intent i = new Intent(ctx, XKReceiver.class).setAction(XKReceiver.ACTION_REMIND).putExtra("kind", k).putExtra("slot", slot);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 100 + slot * 3 + k, i, flags);
            am.cancel(pi);
            if (!remind() || expire <= 0) continue;
            long fire = expire - before[k];
            if (before[k] > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(fire);
                cal.set(Calendar.HOUR_OF_DAY, 12); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
                fire = cal.getTimeInMillis();
            }
            if (fire - System.currentTimeMillis() < 60_000) continue;
            am.set(AlarmManager.RTC_WAKEUP, fire, pi);
        }
    }
}
