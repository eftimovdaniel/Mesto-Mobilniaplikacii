package com.example.mesto_samostojna.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.mesto_samostojna.R;
import com.example.mesto_samostojna.data.Company;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * BroadcastReceiver za geofence ENTER (&lt; 50 m).
 *
 * ZOSO Receiver: se aktivira i koga app ne e vo foreground (OS go budi).
 * KAKO RABOTI: GeofencingEvent → ENTER → Toast (zadaca) + ProximityNotifier
 * (sistemska notifikacija; tap otvora CompanyDetailActivity).
 */
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
            if (company == null) {
                continue;
            }
            // Toast — zadacata bara Toast poraka (i koga app e vklucena).
            Toast.makeText(
                            context,
                            context.getString(R.string.proximity_near, company.getName()),
                            Toast.LENGTH_LONG)
                    .show();
            // Notifikacija — rezerva koga UI ne e vo foreground.
            ProximityNotifier.showEnter(context, company);
        }
    }
}
