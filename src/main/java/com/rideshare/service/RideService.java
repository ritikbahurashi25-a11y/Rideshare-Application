package com.rideshare.service;

import com.rideshare.dto.CancelRideRequest;
import com.rideshare.dto.RideRequestDTO;
import com.rideshare.dto.RideResponseDTO;
import com.rideshare.entity.Ride;
import com.rideshare.entity.RideStatus;
import com.rideshare.entity.Role;
import com.rideshare.entity.User;
import com.rideshare.exception.InvalidRideStateException;
import com.rideshare.exception.ResourceNotFoundException;
import com.rideshare.repository.RideRepository;
import com.rideshare.repository.UserRepository;
import com.rideshare.util.DistanceUtil;
import com.rideshare.websocket.RealtimeBroadcaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final MatchingService matchingService;
    private final FareCalculationService fareCalculationService;
    private final RealtimeBroadcaster broadcaster;
    private final PaymentService paymentService;

    public RideService(RideRepository rideRepository, UserRepository userRepository,
                        MatchingService matchingService, FareCalculationService fareCalculationService,
                        RealtimeBroadcaster broadcaster, PaymentService paymentService) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.matchingService = matchingService;
        this.fareCalculationService = fareCalculationService;
        this.broadcaster = broadcaster;
        this.paymentService = paymentService;
    }

    @Transactional
    public RideResponseDTO requestRide(RideRequestDTO req) {
        User rider = userRepository.findById(req.getRiderId())
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found: " + req.getRiderId()));
        if (rider.getRole() != Role.RIDER) {
            throw new IllegalArgumentException("User " + req.getRiderId() + " is not a rider");
        }

        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setPickupLat(req.getPickupLat());
        ride.setPickupLng(req.getPickupLng());
        ride.setDropLat(req.getDropLat());
        ride.setDropLng(req.getDropLng());
        ride.setVehicleType(req.getVehicleType());
        ride.setStatus(RideStatus.REQUESTED);

        ride = rideRepository.save(ride);

        return attemptMatch(ride);
    }

    /** Tries to find and assign the nearest available driver to a REQUESTED ride. */
    private RideResponseDTO attemptMatch(Ride ride) {
        Optional<User> match = matchingService.findNearestAvailableDriver(ride);

        if (match.isPresent()) {
            User driver = match.get();
            ride.setDriver(driver);
            ride.setStatus(RideStatus.DRIVER_ASSIGNED);
            // Tentatively reserve the driver so they aren't matched to two rides at once
            driver.setAvailable(false);
            userRepository.save(driver);
        } else {
            ride.setStatus(RideStatus.NO_DRIVER_FOUND);
        }

        Ride saved = rideRepository.save(ride);
        RideResponseDTO dto = RideResponseDTO.fromEntity(saved);
        broadcaster.broadcastRideUpdate(dto);
        return dto;
    }

    /** Rider (or a scheduler) can retry matching if NO_DRIVER_FOUND. */
    @Transactional
    public RideResponseDTO retryMatch(Long rideId) {
        Ride ride = getRideEntity(rideId);
        if (ride.getStatus() != RideStatus.NO_DRIVER_FOUND) {
            throw new InvalidRideStateException("Ride is not in NO_DRIVER_FOUND state");
        }
        return attemptMatch(ride);
    }

    @Transactional
    public RideResponseDTO acceptRide(Long rideId, Long driverId) {
        Ride ride = getRideEntity(rideId);
        validateDriverOwnsRide(ride, driverId);
        if (ride.getStatus() != RideStatus.DRIVER_ASSIGNED) {
            throw new InvalidRideStateException("Ride cannot be accepted from status " + ride.getStatus());
        }
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);

        RideResponseDTO dto = RideResponseDTO.fromEntity(saved);
        broadcaster.broadcastRideUpdate(dto);
        return dto;
    }

    @Transactional
    public RideResponseDTO rejectRide(Long rideId, Long driverId) {
        Ride ride = getRideEntity(rideId);
        validateDriverOwnsRide(ride, driverId);
        if (ride.getStatus() != RideStatus.DRIVER_ASSIGNED) {
            throw new InvalidRideStateException("Ride cannot be rejected from status " + ride.getStatus());
        }

        // free up the rejecting driver, add to exclusion list, and re-match
        User driver = ride.getDriver();
        driver.setAvailable(true);
        userRepository.save(driver);

        String rejected = ride.getRejectedDriverIds();
        ride.setRejectedDriverIds((rejected == null || rejected.isBlank() ? "" : rejected + ",") + driverId);
        ride.setDriver(null);
        ride.setStatus(RideStatus.REQUESTED);
        rideRepository.save(ride);

        return attemptMatch(ride);
    }

    @Transactional
    public RideResponseDTO startRide(Long rideId, Long driverId) {
        Ride ride = getRideEntity(rideId);
        validateDriverOwnsRide(ride, driverId);
        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidRideStateException("Ride cannot be started from status " + ride.getStatus());
        }
        ride.setStatus(RideStatus.ONGOING);
        ride.setStartedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);

        RideResponseDTO dto = RideResponseDTO.fromEntity(saved);
        broadcaster.broadcastRideUpdate(dto);
        return dto;
    }

    @Transactional
    public RideResponseDTO completeRide(Long rideId, Long driverId) {
        Ride ride = getRideEntity(rideId);
        validateDriverOwnsRide(ride, driverId);
        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new InvalidRideStateException("Ride cannot be completed from status " + ride.getStatus());
        }

        double distanceKm = DistanceUtil.haversineKm(
                ride.getPickupLat(), ride.getPickupLng(), ride.getDropLat(), ride.getDropLng());

        LocalDateTime completedAt = LocalDateTime.now();
        double durationMin;
        if (ride.getStartedAt() != null) {
            durationMin = ChronoUnit.SECONDS.between(ride.getStartedAt(), completedAt) / 60.0;
            if (durationMin <= 0) {
                durationMin = fareCalculationService.estimateDurationMin(distanceKm);
            }
        } else {
            durationMin = fareCalculationService.estimateDurationMin(distanceKm);
        }

        double fare = fareCalculationService.calculateFare(distanceKm, durationMin, ride.getVehicleType());

        ride.setDistanceKm(round2(distanceKm));
        ride.setDurationMin(round2(durationMin));
        ride.setFare(fare);
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(completedAt);
        Ride saved = rideRepository.save(ride);

        // free up the driver for new rides
        User driver = ride.getDriver();
        driver.setAvailable(true);
        userRepository.save(driver);

        // create a PENDING payment record the rider can now pay
        paymentService.createPendingPayment(saved);

        RideResponseDTO dto = RideResponseDTO.fromEntity(saved);
        broadcaster.broadcastRideUpdate(dto);
        return dto;
    }

    @Transactional
    public RideResponseDTO cancelRide(Long rideId, CancelRideRequest req) {
        Ride ride = getRideEntity(rideId);
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideStateException("Ride cannot be cancelled from status " + ride.getStatus());
        }

        if (ride.getDriver() != null) {
            User driver = ride.getDriver();
            driver.setAvailable(true);
            userRepository.save(driver);
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelReason(req != null ? req.getReason() : null);
        Ride saved = rideRepository.save(ride);

        RideResponseDTO dto = RideResponseDTO.fromEntity(saved);
        broadcaster.broadcastRideUpdate(dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public Ride getRideEntity(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));
    }

    @Transactional(readOnly = true)
    public RideResponseDTO getRide(Long rideId) {
        return RideResponseDTO.fromEntity(getRideEntity(rideId));
    }

    @Transactional(readOnly = true)
    public List<RideResponseDTO> getRidesByRider(Long riderId) {
        return rideRepository.findByRiderIdOrderByRequestedAtDesc(riderId).stream()
                .map(RideResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RideResponseDTO> getRidesByDriver(Long driverId) {
        return rideRepository.findByDriverIdOrderByRequestedAtDesc(driverId).stream()
                .map(RideResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /** Returns the driver's currently active ride (assigned/accepted/ongoing), if any. */
    @Transactional(readOnly = true)
    public Optional<RideResponseDTO> getActiveRideForDriver(Long driverId) {
        return rideRepository.findByDriverIdOrderByRequestedAtDesc(driverId).stream()
                .filter(r -> r.getStatus() == RideStatus.DRIVER_ASSIGNED
                        || r.getStatus() == RideStatus.ACCEPTED
                        || r.getStatus() == RideStatus.ONGOING)
                .findFirst()
                .map(RideResponseDTO::fromEntity);
    }

    private void validateDriverOwnsRide(Ride ride, Long driverId) {
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
            throw new IllegalArgumentException("Driver " + driverId + " is not assigned to ride " + ride.getId());
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
