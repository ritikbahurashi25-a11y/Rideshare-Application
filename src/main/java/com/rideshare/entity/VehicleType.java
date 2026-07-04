package com.rideshare.entity;

public enum VehicleType {
    BIKE(1.0),
    AUTO(1.2),
    SEDAN(1.6),
    SUV(2.0);

    private final double fareMultiplier;

    VehicleType(double fareMultiplier) {
        this.fareMultiplier = fareMultiplier;
    }

    public double getFareMultiplier() {
        return fareMultiplier;
    }
}
