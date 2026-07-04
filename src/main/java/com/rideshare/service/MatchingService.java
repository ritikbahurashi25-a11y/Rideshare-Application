package com.rideshare.service;

import com.rideshare.entity.Ride;
import com.rideshare.entity.Role;
import com.rideshare.entity.User;
import com.rideshare.entity.VehicleType;
import com.rideshare.repository.UserRepository;
import com.rideshare.util.DistanceUtil;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles rider <-> driver matching.
 *
 * Strategy: fetch all available drivers, filter by requested vehicle type
 * and a max search radius, exclude drivers already rejected for this ride,
 * then pick the nearest one using Haversine distance.
 *
 * This is intentionally simple (O(n) scan in Java) which is fine for a
 * learning project / moderate driver counts. For production scale this
 * would be replaced by a geospatial index (MySQL spatial types, Redis GEO,
 * or a dedicated geo-matching service).
 */
@Service
public class MatchingService {

    private static final double MAX_SEARCH_RADIUS_KM = 10.0;

    private final UserRepository userRepository;

    public MatchingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findNearestAvailableDriver(Ride ride) {
        Set<Long> rejectedIds = parseRejectedIds(ride.getRejectedDriverIds());

        List<User> candidates = userRepository.findByRoleAndAvailableTrue(Role.DRIVER).stream()
                .filter(d -> !rejectedIds.contains(d.getId()))
                .filter(d -> d.getCurrentLat() != null && d.getCurrentLng() != null)
                .filter(d -> d.getVehicleType() == ride.getVehicleType())
                .filter(d -> DistanceUtil.haversineKm(
                        ride.getPickupLat(), ride.getPickupLng(),
                        d.getCurrentLat(), d.getCurrentLng()) <= MAX_SEARCH_RADIUS_KM)
                .collect(Collectors.toList());

        return candidates.stream()
                .min((a, b) -> Double.compare(
                        DistanceUtil.haversineKm(ride.getPickupLat(), ride.getPickupLng(), a.getCurrentLat(), a.getCurrentLng()),
                        DistanceUtil.haversineKm(ride.getPickupLat(), ride.getPickupLng(), b.getCurrentLat(), b.getCurrentLng())
                ));
    }

    private Set<Long> parseRejectedIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }
}
