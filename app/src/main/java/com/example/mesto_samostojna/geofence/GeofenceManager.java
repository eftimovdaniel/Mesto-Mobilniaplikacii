package com.example.mesto_samostojna.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.mesto_samostojna.data.Company;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Upravuva so Google Play Services geofences.
 *
 * ZOSO geofence (ne samo FusedLocation): koga app e vo background,
 * OS seuste moze da go izvesti receiver-ot pri vlez vo zonata.
 *
 * KAKO RABOTI:
 * - Za sekoja kompanija registrira krug od 50 m.
 * - Pri ENTER → GeofenceBroadcastReceiver (Toast + notifikacija).
 * - syncGeofences: prvo remove stari, pa add novi (bez duplikati).
 * - GeofenceCompanyStore cuva id→Company lokalno za ime vo notifikacijata.
 */
public final class GeofenceManager {

    private static final String TAG = "GeofenceManager";
    /** Radius na geofence — ist kako vo baranjeto na zadacata (pod 50 m). */
    public static final float RADIUS_METERS = 50f;
    private static final long EXPIRATION_MS = Geofence.NEVER_EXPIRE;

    private GeofenceManager() {}

    /** Dali imame barem fine ili coarse location permission. */
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Brise stari geofences i registrira novi spored tekovnata lista.
     * Zoshto prvo remove: da nema duplikati posle reload.
     * Zacuva i vo GeofenceCompanyStore — receiver-ot treba ime/id pri ENTER.
     */
    public static void syncGeofences(Context context, List<Company> companies) {
        if (!hasLocationPermission(context)) {
            return;
        }

        GeofencingClient client = LocationServices.getGeofencingClient(context);
        PendingIntent pendingIntent = geofencePendingIntent(context);

        // Prvo ostrani stari, pa dodaj novi (bez duplikati po reload)
        client.removeGeofences(pendingIntent)
                .addOnCompleteListener(
                        task -> {
                            GeofenceCompanyStore.saveAll(context, companies);
                            List<Geofence> geofences = buildGeofences(companies);
                            if (geofences.isEmpty()) {
                                Log.d(TAG, "No geofences to register.");
                                return;
                            }
                            GeofencingRequest request =
                                    new GeofencingRequest.Builder()
                                            .setInitialTrigger(
                                                    GeofencingRequest.INITIAL_TRIGGER_ENTER)
                                            .addGeofences(geofences)
                                            .build();
                            client.addGeofences(request, pendingIntent)
                                    .addOnSuccessListener(
                                            unused ->
                                                    Log.d(
                                                            TAG,
                                                            "Registered "
                                                                    + geofences.size()
                                                                    + " geofences."))
                                    .addOnFailureListener(
                                            e ->
                                                    Log.e(
                                                            TAG,
                                                            "addGeofences failed",
                                                            e));
                        });
    }

    /**
     * Pretvora lista Company vo lista Geofence objekti (krug od 50 m sekoj).
     * ZOSO se preskoknuvaat kompanii bez id: requestId mora da e unikaten za
     * podocna da ja povrzeme notifikacijata so pravata kompanija.
     * setTransitionTypes = ENTER samo — ne ni trebaat EXIT/DWELL nastani.
     */
    private static List<Geofence> buildGeofences(List<Company> companies) {
        List<Geofence> list = new ArrayList<>();
        if (companies == null) {
            return list;
        }
        for (Company c : companies) {
            if (c.getId() == null) {
                continue;
            }
            list.add(
                    new Geofence.Builder()
                            .setRequestId(GeofenceCompanyStore.requestIdFor(c.getId()))
                            .setCircularRegion(
                                    c.getLatitude(), c.getLongitude(), RADIUS_METERS)
                            .setExpirationDuration(EXPIRATION_MS)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                            .build());
        }
        return list;
    }

    /**
     * PendingIntent sto OS go "pali" pri geofence nastan → GeofenceBroadcastReceiver.
     *
     * ZOSO FLAG_MUTABLE na Android 12+ (S): Play Services mora da vmetne detali
     * za nastanot (koj geofence, ENTER/EXIT) vo Intent-ot — zatoa ne smee da e
     * immutable. Na postari verzii se koristi IMMUTABLE (baranje na sistemot).
     */
    private static PendingIntent geofencePendingIntent(Context context) {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        } else {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }
}
