package com.rideshare.controller;

import com.rideshare.dto.CancelRideRequest;
import com.rideshare.dto.RideRequestDTO;
import com.rideshare.dto.RideResponseDTO;
import com.rideshare.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    /** Rider requests a ride. Automatically attempts to match the nearest available driver. */
    @PostMapping("/request")
    public ResponseEntity<RideResponseDTO> requestRide(@Valid @RequestBody RideRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.requestRide(req));
    }

    /** Retry matching for a ride stuck in NO_DRIVER_FOUND state. */
    @PostMapping("/{rideId}/retry-match")
    public ResponseEntity<RideResponseDTO> retryMatch(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.retryMatch(rideId));
    }

    @PostMapping("/{rideId}/accept")
    public ResponseEntity<RideResponseDTO> accept(@PathVariable Long rideId, @RequestParam Long driverId) {
        return ResponseEntity.ok(rideService.acceptRide(rideId, driverId));
    }

    @PostMapping("/{rideId}/reject")
    public ResponseEntity<RideResponseDTO> reject(@PathVariable Long rideId, @RequestParam Long driverId) {
        return ResponseEntity.ok(rideService.rejectRide(rideId, driverId));
    }

    @PostMapping("/{rideId}/start")
    public ResponseEntity<RideResponseDTO> start(@PathVariable Long rideId, @RequestParam Long driverId) {
        return ResponseEntity.ok(rideService.startRide(rideId, driverId));
    }

    @PostMapping("/{rideId}/complete")
    public ResponseEntity<RideResponseDTO> complete(@PathVariable Long rideId, @RequestParam Long driverId) {
        return ResponseEntity.ok(rideService.completeRide(rideId, driverId));
    }

    @PostMapping("/{rideId}/cancel")
    public ResponseEntity<RideResponseDTO> cancel(@PathVariable Long rideId,
                                                    @RequestBody(required = false) CancelRideRequest req) {
        return ResponseEntity.ok(rideService.cancelRide(rideId, req));
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponseDTO> getRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.getRide(rideId));
    }

    @GetMapping("/rider/{riderId}")
    public ResponseEntity<List<RideResponseDTO>> getRidesByRider(@PathVariable Long riderId) {
        return ResponseEntity.ok(rideService.getRidesByRider(riderId));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<RideResponseDTO>> getRidesByDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(rideService.getRidesByDriver(driverId));
    }
}
