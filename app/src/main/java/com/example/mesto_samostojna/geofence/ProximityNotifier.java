package com.example.mesto_samostojna.geofence;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.mesto_samostojna.CompanyDetailActivity;
import com.example.mesto_samostojna.R;
import com.example.mesto_samostojna.data.Company;

/**
 * Sistemska notifikacija pri vlez vo geofence okolu kompanija.
 * ZOSO: koga app e vo background, Toast moze da ne se vidi — notifikacijata e rezerva.
 * Tap na notifikacija → CompanyDetailActivity za taa kompanija.
 */
public final class ProximityNotifier {

    private static final String CHANNEL_ID = "mesto_proximity";
    private static final int NOTIFICATION_BASE_ID = 40_000;

    private ProximityNotifier() {}

    /**
     * Kreira Notification Channel (zadolzitelno od Android 8/Oreo navamu).
     * ZOSO: bez kanal, notifikaciite ednostavno ne se prikazuvaat na 8.0+.
     * Bezbedno e da se povika povekjepati — sistemot go ignorira ako veke postoi.
     */
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.geofence_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.geofence_channel_desc));
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(channel);
        }
    }

    /**
     * Prikazuva notifikacija "blizu si do {kompanija}", so tap sto vodi kon
     * detalen ekran za taa kompanija.
     *
     * ZOSO proverka na POST_NOTIFICATIONS (Android 13+): bez taa dozvola
     * notify() e tivko ignoriran — proverkata sprecuva bezmisleni obidi.
     * ZOSO unikaten notifId po kompanija: dve razlicni kompanii da dadat dve
     * oddelni notifikacii (ne da se prepišuvaat).
     */
    public static void showEnter(Context context, Company company) {
        ensureChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Intent sto se otvora pri tap: nosi ja celata Company (Serializable).
        // NEW_TASK + CLEAR_TOP — se otvora korektno i koga app ne e vo foreground.
        Intent open =
                new Intent(context, CompanyDetailActivity.class)
                        .putExtra(CompanyDetailActivity.EXTRA_COMPANY, company)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = company.getId() != null ? company.getId() : 0;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent contentIntent =
                PendingIntent.getActivity(context, requestCode, open, flags);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_location_pin)
                        .setContentTitle(context.getString(R.string.geofence_notif_title))
                        .setContentText(
                                context.getString(R.string.geofence_notif_body, company.getName()))
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(
                                                context.getString(
                                                        R.string.geofence_notif_big,
                                                        company.getName(),
                                                        company.getAddress())))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(contentIntent);

        int notifId = NOTIFICATION_BASE_ID + (company.getId() != null ? company.getId() : 0);
        NotificationManagerCompat.from(context).notify(notifId, builder.build());
    }
}
