package app.meows.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

/** Expiry reminders + re-scheduling after reboot. */
public class XKReceiver extends BroadcastReceiver {
    public static final String ACTION_REMIND = "app.meows.vpn.REMIND";

    @Override public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
            for (XKStore st : XKStore.all(c)) st.scheduleReminders();
            return;
        }
        if (!ACTION_REMIND.equals(i.getAction())) return;
        int kind = i.getIntExtra("kind", 0);
        XKStore st = XKStore.byId(c, i.getIntExtra("slot", 0));
        String who = XKStore.slotIds(c).size() > 1 ? "«" + st.title() + "»: " : "";   // с несколькими подписками — чья именно
        String[] titles = {"Подписка скоро закончится", "Завтра заканчивается подписка", "Подписка закончилась"};
        String[] bodies = {
                "Через 3 дня подписка закончится. Нажмите, чтобы продлить в личном кабинете.",
                "Остался 1 день. Продлите подписку, чтобы VPN не отключился.",
                "Подписка закончилась. Продлите её в личном кабинете и нажмите «Обновить» в приложении."};
        if (app.meows.vpn.Edition.isPlay) bodies = new String[]{
                "Через 3 дня подписка закончится.",
                "Остался 1 день до конца подписки.",
                "Подписка закончилась. После продления нажмите «Обновить» в приложении."};
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel("remind") == null) {
            nm.createNotificationChannel(new NotificationChannel("remind", "Напоминания о подписке", NotificationManager.IMPORTANCE_HIGH));
        }
        int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(c, 7 + kind + 20 * st.id(), cabinetIntent(c, st), fl);   // нажатие — сразу в кабинет, продлить
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, "remind") : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat).setContentTitle(titles[kind]).setContentText(who + bodies[kind])
                .setStyle(new Notification.BigTextStyle().bigText(who + bodies[kind]))
                .setColor(0xFF7CD62B).setAutoCancel(true).setContentIntent(pi);
        nm.notify(10 + kind + 100 * st.id(), b.build());
    }

    private static Intent cabinetIntent(Context c, XKStore st) {
        if (app.meows.vpn.Edition.isPlay) {   // в Play-версии ссылок на оплату нет: нажатие просто открывает приложение
            Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
            if (open != null) return open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        String url = app.meows.vpn.shop.ShopLinks.cabinet(st);
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** Предупреждение о трафике: один раз на 90 % и один раз на 100 % в рамках текущего срока подписки (проверка при каждом обновлении подписки). */
    public static void checkTraffic(Context c, XKStore st) {
        long total = st.total();
        if (!st.remind() || total <= 0) return;
        int pct = (int) Math.min(100, st.used() * 100 / total);
        int level = pct >= 100 ? 100 : (pct >= 90 ? 90 : 0);
        String key = st.expire() + ":" + level;   // у каждой подписки свой счётчик (ключ trafficWarn<слот>)
        android.content.SharedPreferences sp = st.prefs();
        String wk = "trafficWarn" + st.id();
        if (level == 0) { sp.edit().remove(wk).apply(); return; }   // новый период — снова можно предупреждать
        if (key.equals(sp.getString(wk, ""))) return;
        sp.edit().putString(wk, key).apply();
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel("remind") == null) {
            nm.createNotificationChannel(new NotificationChannel("remind", "Напоминания о подписке", NotificationManager.IMPORTANCE_HIGH));
        }
        String title = level == 100 ? "Трафик закончился" : "Трафик почти закончился";
        String body = app.meows.vpn.Edition.isPlay
                ? (level == 100 ? "Лимит трафика исчерпан." : "Использовано " + pct + "% трафика.")
                : (level == 100 ? "Лимит исчерпан. Продлите или докупите трафик в личном кабинете." : "Использовано " + pct + "% трафика. Нажмите, чтобы открыть личный кабинет.");
        int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, "remind") : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(0xFF7CD62B).setAutoCancel(true).setContentIntent(PendingIntent.getActivity(c, 8 + 20 * st.id(), cabinetIntent(c, st), fl));
        nm.notify(20 + 100 * st.id(), b.build());
    }
}
