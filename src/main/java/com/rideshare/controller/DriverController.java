package com.rideshare.controller;

import com.rideshare.dto.AvailabilityRequest;
import com.rideshare.dto.LocationUpdateRequest;
import com.rideshare.dto.UserResponseDTO;
import com.rideshare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final UserService userService;

    public DriverController(UserService userService) {
        this.userService = userService;
    }

    /** Driver app calls this periodically (e.g. every few seconds) to push live GPS. */
    @PutMapping("/{driverId}/location")
    public ResponseEntity<UserResponseDTO> updateLocation(@PathVariable Long driverId,
                                                            @Valid @RequestBody LocationUpdateRequest req) {
        return ResponseEntity.ok(userService.updateLocation(driverId, req));
    }

    /** Driver toggles online/offline status. */
    @PutMapping("/{driverId}/availability")
    public ResponseEntity<UserResponseDTO> setAvailability(@PathVariable Long driverId,
                                                             @Valid @RequestBody AvailabilityRequest req) {
        return ResponseEntity.ok(userService.setAvailability(driverId, req));
    }

    /** Lists nearby available drivers - useful for a rider-facing "drivers near you" map. */
    @GetMapping("/nearby")
    public ResponseEntity<List<UserResponseDTO>> nearbyDrivers(@RequestParam double lat,
                                                                 @RequestParam double lng,
                                                                 @RequestParam(defaultValue = "5") double radiusKm) {
        return ResponseEntity.ok(userService.findNearbyAvailableDrivers(lat, lng, radiusKm));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<UserResponseDTO> getDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(userService.getUser(driverId));
    }
}
