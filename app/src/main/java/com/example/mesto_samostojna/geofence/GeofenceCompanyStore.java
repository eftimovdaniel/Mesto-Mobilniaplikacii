package com.example.mesto_samostojna.geofence;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.example.mesto_samostojna.data.Company;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lokalen kesh: geofence requestId → Company (SharedPreferences + Gson).
 *
 * ZOSO: koga GeofenceBroadcastReceiver se aktivira vo background,
 * nema pristap do listata vo MainActivity — mora lokalno cuvanje
 * za da ja najde ime/id za Toast i notifikacija.
 */
public final class GeofenceCompanyStore {

    private static final String PREFS = "geofence_companies";
    private static final String KEY_JSON = "map";
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Company>>() {}.getType();

    private GeofenceCompanyStore() {}

    /**
     * Geofence-ite se identifikuvaat po String requestId, a nasite kompanii po
     * int id — ovaa metoda go pravi mapiranjeto (id 7 → "c_7"). Prefiksot "c_"
     * gi razlikuva nasite geofences od bilo koi drugi vo sistemot.
     */
    public static String requestIdFor(int companyId) {
        return "c_" + companyId;
    }

    /** Obratno od requestIdFor: "c_7" → 7; null ako formatot ne e nas. */
    @Nullable
    public static Integer companyIdFromRequestId(String requestId) {
        if (requestId == null || !requestId.startsWith("c_")) {
            return null;
        }
        try {
            return Integer.parseInt(requestId.substring(2));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Serijalizira mapata requestId→Company vo JSON (Gson) i ja cuva vo
     * SharedPreferences. Se povikuva pri sekoj syncGeofences za da e keshot
     * sinhron so geofence-ite registrirani vo OS.
     */
    public static void saveAll(Context context, List<Company> companies) {
        Map<String, Company> map = new HashMap<>();
        if (companies != null) {
            for (Company c : companies) {
                if (c.getId() != null) {
                    map.put(requestIdFor(c.getId()), c);
                }
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_JSON, GSON.toJson(map))
                .apply();
    }

    /** Go vraka Company objektot vrzan za daden geofence requestId (ili null). */
    @Nullable
    public static Company getByRequestId(Context context, String requestId) {
        Map<String, Company> map = loadMap(context);
        return map.get(requestId);
    }

    /** Deserijalizira JSON nazad vo mapa; prazna mapa ako nema/nevaliden zapis. */
    private static Map<String, Company> loadMap(Context context) {
        String json =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_JSON, "{}");
        Map<String, Company> map = GSON.fromJson(json, MAP_TYPE);
        return map != null ? map : new HashMap<>();
    }
}
