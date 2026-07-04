package com.rideshare.service;

import com.rideshare.entity.VehicleType;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

/**
 * Fare = baseFare + (distanceKm * perKmRate) + (durationMin * perMinRate)
 *        all multiplied by vehicleType multiplier and a surge multiplier,
 *        then floored at a minimum fare.
 *
 * Currency: INR (â‚¹) - adjust constants as needed.
 */
@Service
public class FareCalculationService {

    private static final double BASE_FARE = 40.0;
    private static final double PER_KM_RATE = 12.0;
    private static final double PER_MIN_RATE = 1.5;
    private static final double MINIMUM_FARE = 60.0;

    // Simple demand-based surge windows (peak hours)
    private static final double SURGE_MULTIPLIER = 1.5;

    public double calculateFare(double distanceKm, double durationMin, VehicleType vehicleType) {
        double raw = BASE_FARE + (distanceKm * PER_KM_RATE) + (durationMin * PER_MIN_RATE);
        double withVehicleMultiplier = raw * vehicleType.getFareMultiplier();
        double withSurge = withVehicleMultiplier * getCurrentSurgeMultiplier();
        return Math.max(MINIMUM_FARE, round2(withSurge));
    }

    /**
     * Returns a surge multiplier based on time of day.
     * Peak hours (8-10 AM, 6-9 PM): 1.5x. Otherwise 1.0x.
     */
    public double getCurrentSurgeMultiplier() {
        LocalTime now = LocalTime.now();
        boolean morningPeak = !now.isBefore(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(10, 0));
        boolean eveningPeak = !now.isBefore(LocalTime.of(18, 0)) && now.isBefore(LocalTime.of(21, 0));
        return (morningPeak || eveningPeak) ? SURGE_MULTIPLIER : 1.0;
    }

    /** Estimated duration in minutes assuming average city speed of 30 km/h. */
    public double estimateDurationMin(double distanceKm) {
        double avgSpeedKmh = 30.0;
        return round2((distanceKm / avgSpeedKmh) * 60.0);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
