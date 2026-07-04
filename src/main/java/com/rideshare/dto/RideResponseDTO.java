package com.rideshare.dto;

import com.rideshare.entity.Ride;
import com.rideshare.entity.RideStatus;
import com.rideshare.entity.VehicleType;

import java.time.LocalDateTime;

public class RideResponseDTO {

    private Long id;
    private Long riderId;
    private String riderName;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;

    private Double pickupLat;
    private Double pickupLng;
    private Double dropLat;
    private Double dropLng;

    private VehicleType vehicleType;
    private RideStatus status;

    private Double distanceKm;
    private Double durationMin;
    private Double fare;

    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public static RideResponseDTO fromEntity(Ride ride) {
        RideResponseDTO dto = new RideResponseDTO();
        dto.id = ride.getId();
        dto.riderId = ride.getRider() != null ? ride.getRider().getId() : null;
        dto.riderName = ride.getRider() != null ? ride.getRider().getName() : null;
        if (ride.getDriver() != null) {
            dto.driverId = ride.getDriver().getId();
            dto.driverName = ride.getDriver().getName();
            dto.driverPhone = ride.getDriver().getPhone();
            dto.vehicleNumber = ride.getDriver().getVehicleNumber();
        }
        dto.pickupLat = ride.getPickupLat();
        dto.pickupLng = ride.getPickupLng();
        dto.dropLat = ride.getDropLat();
        dto.dropLng = ride.getDropLng();
        dto.vehicleType = ride.getVehicleType();
        dto.status = ride.getStatus();
        dto.distanceKm = ride.getDistanceKm();
        dto.durationMin = ride.getDurationMin();
        dto.fare = ride.getFare();
        dto.requestedAt = ride.getRequestedAt();
        dto.acceptedAt = ride.getAcceptedAt();
        dto.startedAt = ride.getStartedAt();
        dto.completedAt = ride.getCompletedAt();
        return dto;
    }

    // ---------- Getters & Setters ----------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRiderId() {
        return riderId;
    }

    public void setRiderId(Long riderId) {
        this.riderId = riderId;
    }

    public String getRiderName() {
        return riderName;
    }

    public void setRiderName(String riderName) {
        this.riderName = riderName;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public Double getPickupLat() {
        return pickupLat;
    }

    public void setPickupLat(Double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public Double getPickupLng() {
        return pickupLng;
    }

    public void setPickupLng(Double pickupLng) {
        this.pickupLng = pickupLng;
    }

    public Double getDropLat() {
        return dropLat;
    }

    public void setDropLat(Double dropLat) {
        this.dropLat = dropLat;
    }

    public Double getDropLng() {
        return dropLng;
    }

    public void setDropLng(Double dropLng) {
        this.dropLng = dropLng;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(Double durationMin) {
        this.durationMin = durationMin;
    }

    public Double getFare() {
        return fare;
    }

    public void setFare(Double fare) {
        this.fare = fare;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
