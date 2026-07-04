package com.rideshare.websocket;

import com.rideshare.dto.RideResponseDTO;
import com.rideshare.dto.UserResponseDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Central place for pushing real-time events to connected WebSocket clients.
 * Topics:
 *  - /topic/driver-location.{driverId}
 *  - /topic/ride.{rideId}
 */
@Component
public class RealtimeBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastDriverLocation(UserResponseDTO driver) {
        messagingTemplate.convertAndSend("/topic/driver-location." + driver.getId(), driver);
    }

    public void broadcastRideUpdate(RideResponseDTO ride) {
        messagingTemplate.convertAndSend("/topic/ride." + ride.getId(), ride);
        // Also notify the assigned driver's personal channel (useful for new ride offers)
        if (ride.getDriverId() != null) {
            messagingTemplate.convertAndSend("/topic/driver-rides." + ride.getDriverId(), ride);
        }
    }
}
