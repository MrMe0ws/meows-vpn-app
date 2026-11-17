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
            XKStore.get(c).scheduleReminders();
            return;
        }
        if (!ACTION_REMIND.equals(i.getAction())) return;
        int kind = i.getIntExtra("kind", 0);
        String[] titles = {"Подписка скоро закончится", "Завтра заканчивается подписка", "Подписка закончилась"};
        String[] bodies = {
                "Через 3 дня подписка закончится. Продлите её у вашего провайдера.",
                "Остался 1 день. Продлите подписку, чтобы VPN не отключился.",
                "Подписка закончилась. Продлите её и нажмите «Обновить»."};
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel("remind") == null) {
            nm.createNotificationChannel(new NotificationChannel("remind", "Напоминания о подписке", NotificationManager.IMPORTANCE_HIGH));
        }
        int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(c, 7, new Intent(c, MainActivity.class), fl);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, "remind") : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat).setContentTitle(titles[kind]).setContentText(bodies[kind])
                .setStyle(new Notification.BigTextStyle().bigText(bodies[kind]))
                .setColor(0xFF7CD62B).setAutoCancel(true).setContentIntent(pi);
        nm.notify(10 + kind, b.build());
    }
}
