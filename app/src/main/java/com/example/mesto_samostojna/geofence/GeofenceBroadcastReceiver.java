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
 * Geofence ENTER (&lt; 50 m): Toast (барање од задачата) + нотификација како резерва
 * за кога апликацијата е затворена.
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
            // Toast — задачата бара да се прикаже Toast порака кога апликацијата е вклучена.
            Toast.makeText(
                            context,
                            context.getString(R.string.proximity_near, company.getName()),
                            Toast.LENGTH_LONG)
                    .show();
            // Нотификација — за случај кога UI-то не е во foreground.
            ProximityNotifier.showEnter(context, company);
        }
    }
}
