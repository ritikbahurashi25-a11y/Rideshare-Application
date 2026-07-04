package com.rideshare.entity;

public enum RideStatus {
    REQUESTED,        // rider just requested a ride
    DRIVER_ASSIGNED,  // a driver has been matched and notified
    ACCEPTED,         // driver accepted the ride
    ONGOING,          // ride in progress (driver picked up rider)
    COMPLETED,        // ride finished, fare calculated
    CANCELLED,        // cancelled by rider or driver
    NO_DRIVER_FOUND   // no available driver within radius
}
