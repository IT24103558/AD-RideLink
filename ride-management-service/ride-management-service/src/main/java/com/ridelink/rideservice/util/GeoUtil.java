package com.ridelink.rideservice.util;

public final class GeoUtil {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    /** Same constants as the Fare Service so estimates are consistent. */
    public static final double ROAD_FACTOR = 1.25;
    public static final double AVERAGE_SPEED_KMH = 30.0;

    private GeoUtil() {
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    public static double roadDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        return round(haversineKm(lat1, lon1, lat2, lon2) * ROAD_FACTOR, 2);
    }

    public static double durationMinutes(double distanceKm) {
        return round(distanceKm / AVERAGE_SPEED_KMH * 60.0, 1);
    }

    public static double round(double value, int scale) {
        double f = Math.pow(10, scale);
        return Math.round(value * f) / f;
    }
}
