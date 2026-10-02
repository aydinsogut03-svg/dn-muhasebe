package com.dnmarine.muhasebe;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Vade ve vergi günü bildirimleri. Sayfa yaklaşan hatırlatmaların listesini verir (gun, id, baslik, metin, yol);
 * telefon her sabah 09.00 civarında ve uygulama açıldığında o güne kadar gösterilmemiş olanları bildirim olarak gösterir.
 */
public class Hatirlatici extends BroadcastReceiver {

    static final String KANAL = "hatirlatmalar";
    private static final String TERCIH = "hatirlatici";

    @Override
    public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) alarmKur(c);
        bugunuGoster(c);
    }

    /** Listeyi kaydeder, günlük alarmı kurar ve bugünün bildirimlerini hemen gösterir. */
    static void listeKaydet(Context c, String json) {
        c.getSharedPreferences(TERCIH, Context.MODE_PRIVATE).edit().putString("liste", json).apply();
        alarmKur(c);
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 9) bugunuGoster(c);
    }

    static void alarmKur(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Calendar k = Calendar.getInstance();
        k.set(Calendar.HOUR_OF_DAY, 9);
        k.set(Calendar.MINUTE, 0);
        k.set(Calendar.SECOND, 0);
        if (k.getTimeInMillis() <= System.currentTimeMillis()) k.add(Calendar.DAY_OF_MONTH, 1);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, new Intent(c, Hatirlatici.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, k.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    /** Günü gelmiş (bugün ya da dün) ve daha önce gösterilmemiş hatırlatmaları gösterir. */
    static void bugunuGoster(Context c) {
        SharedPreferences t = c.getSharedPreferences(TERCIH, Context.MODE_PRIVATE);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar k = Calendar.getInstance();
        String bugun = f.format(k.getTime());
        k.add(Calendar.DAY_OF_MONTH, -1);
        String dun = f.format(k.getTime());
        Set<String> gosterilen = new HashSet<>(t.getStringSet("gosterilen", new HashSet<>()));
        Set<String> yeni = new HashSet<>();
        try {
            JSONArray l = new JSONArray(t.getString("liste", "[]"));
            for (int n = 0; n < l.length(); n++) {
                JSONObject o = l.getJSONObject(n);
                String gun = o.optString("gun"), id = o.optString("id");
                if (gun.compareTo(bugun) > 0 || gun.compareTo(dun) < 0) continue;
                yeni.add(id);
                if (gosterilen.contains(id)) continue;
                goster(c, id.hashCode(), o.optString("baslik"), o.optString("metin"), o.optString("yol"));
            }
        } catch (Exception e) {
            return;
        }
        // Yalnızca son iki günün kimlikleri tutulur; liste büyümez.
        gosterilen.retainAll(yeni);
        gosterilen.addAll(yeni);
        t.edit().putStringSet("gosterilen", gosterilen).apply();
    }

    static void goster(Context c, int id, String baslik, String metin, String yol) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(KANAL) == null) {
            NotificationChannel k = new NotificationChannel(KANAL, "Vade ve vergi hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT);
            k.setDescription("Fatura vadeleri ve vergi beyan günleri");
            nm.createNotificationChannel(k);
        }
        Intent ac = new Intent(c, MainActivity.class);
        ac.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ac.putExtra("yol", yol);
        PendingIntent pi = PendingIntent.getActivity(c, id, ac, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification b = new Notification.Builder(c, KANAL)
                .setSmallIcon(R.drawable.ic_bildirim)
                .setColor(c.getColor(R.color.vurgu))
                .setContentTitle(baslik)
                .setContentText(metin)
                .setStyle(new Notification.BigTextStyle().bigText(metin))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        try {
            nm.notify(id, b);
        } catch (SecurityException e) {
            // Bildirim izni verilmemiş.
        }
    }
}
