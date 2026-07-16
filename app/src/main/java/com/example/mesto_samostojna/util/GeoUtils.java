package com.example.mesto_samostojna.util;

/**
 * Pomoshna klasa za geo-presmetki.
 *
 * ZOSO Haversine: GPS dava lat/lng (WGS84); treba rastojanie vo metri
 * za da proverime dali korisnikot e pod 50 m od kompanija (Toast / geofence).
 * Dobra za mali rastojanija (grad / lokalen biznis), bez Maps SDK.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000.0; // sreden radius na Zemjata

    private GeoUtils() {}

    /** Vraka rastojanie vo metri megju (lat1,lon1) i (lat2,lon2). */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                                * Math.cos(Math.toRadians(lat2))
                                * Math.sin(dLon / 2)
                                * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }
}
