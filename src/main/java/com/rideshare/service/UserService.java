package com.rideshare.service;

import com.rideshare.dto.*;
import com.rideshare.entity.Role;
import com.rideshare.entity.User;
import com.rideshare.entity.VehicleType;
import com.rideshare.exception.ResourceNotFoundException;
import com.rideshare.repository.UserRepository;
import com.rideshare.util.DistanceUtil;
import com.rideshare.websocket.RealtimeBroadcaster;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RealtimeBroadcaster broadcaster;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        RealtimeBroadcaster broadcaster) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.broadcaster = broadcaster;
    }

    @Transactional
    public UserResponseDTO register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + req.getEmail());
        }
        if (req.getRole() == Role.DRIVER && req.getVehicleType() == null) {
            throw new IllegalArgumentException("vehicleType is required for DRIVER registration");
        }

        User user = new User(req.getName(), req.getEmail(), req.getPhone(),
                passwordEncoder.encode(req.getPassword()), req.getRole());

        if (req.getRole() == Role.DRIVER) {
            user.setVehicleType(req.getVehicleType());
            user.setVehicleNumber(req.getVehicleNumber());
            user.setAvailable(false); // driver must explicitly go online
        }

        User saved = userRepository.save(user);
        return UserResponseDTO.fromEntity(saved);
    }

    public UserResponseDTO login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Invalid email or password");
        }
        return UserResponseDTO.fromEntity(user);
    }

    public User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public UserResponseDTO getUser(Long id) {
        return UserResponseDTO.fromEntity(getUserEntity(id));
    }

    @Transactional
    public UserResponseDTO updateLocation(Long driverId, LocationUpdateRequest req) {
        User user = getUserEntity(driverId);
        user.setCurrentLat(req.getLat());
        user.setCurrentLng(req.getLng());
        User saved = userRepository.save(user);

        UserResponseDTO dto = UserResponseDTO.fromEntity(saved);
        broadcaster.broadcastDriverLocation(dto); // real-time push
        return dto;
    }

    @Transactional
    public UserResponseDTO setAvailability(Long driverId, AvailabilityRequest req) {
        User user = getUserEntity(driverId);
        if (user.getRole() != Role.DRIVER) {
            throw new IllegalArgumentException("Only drivers can toggle availability");
        }
        user.setAvailable(req.getAvailable());
        User saved = userRepository.save(user);
        return UserResponseDTO.fromEntity(saved);
    }

    /**
     * Returns available drivers within radiusKm of the given point, nearest first.
     */
    public List<UserResponseDTO> findNearbyAvailableDrivers(double lat, double lng, double radiusKm) {
        List<User> drivers = userRepository.findByRoleAndAvailableTrue(Role.DRIVER);

        return drivers.stream()
                .filter(d -> d.getCurrentLat() != null && d.getCurrentLng() != null)
                .filter(d -> DistanceUtil.haversineKm(lat, lng, d.getCurrentLat(), d.getCurrentLng()) <= radiusKm)
                .sorted(Comparator.comparingDouble(d ->
                        DistanceUtil.haversineKm(lat, lng, d.getCurrentLat(), d.getCurrentLng())))
                .map(UserResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
