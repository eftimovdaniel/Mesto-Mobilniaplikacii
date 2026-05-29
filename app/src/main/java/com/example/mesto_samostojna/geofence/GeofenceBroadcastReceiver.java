package com.example.mesto_samostojna.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.mesto_samostojna.data.Company;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/** Прима geofence ENTER — прикажува нотификација (работи и во background). */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null) {
            return;
        }
        if (event.hasError()) {
            return;
        }
        if (event.getGeofenceTransition() != Geofence.GEOFENCE_TRANSITION_ENTER) {
            return;
        }

        if (event.getTriggeringGeofences() == null) {
            return;
        }

        ProximityNotifier.ensureChannel(context);

        for (Geofence geofence : event.getTriggeringGeofences()) {
            String requestId = geofence.getRequestId();
            Company company = GeofenceCompanyStore.getByRequestId(context, requestId);
            if (company != null) {
                ProximityNotifier.showEnter(context, company);
            }
        }
    }
}
