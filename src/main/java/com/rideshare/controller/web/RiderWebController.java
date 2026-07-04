package com.rideshare.controller.web;

import com.rideshare.dto.RideRequestDTO;
import com.rideshare.dto.RideResponseDTO;
import com.rideshare.dto.UserResponseDTO;
import com.rideshare.entity.Payment;
import com.rideshare.entity.PaymentStatus;
import com.rideshare.entity.VehicleType;
import com.rideshare.service.PaymentService;
import com.rideshare.service.RideService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/rider")
public class RiderWebController {

    private final RideService rideService;
    private final PaymentService paymentService;

    public RiderWebController(RideService rideService, PaymentService paymentService) {
        this.rideService = rideService;
        this.paymentService = paymentService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");
        model.addAttribute("user", currentUser);
        model.addAttribute("vehicleTypes", VehicleType.values());
        return "rider/dashboard";
    }

    @PostMapping("/book")
    public String bookRide(HttpSession session, Model model,
                            @RequestParam(required = false) Double pickupLat, @RequestParam(required = false) Double pickupLng,
                            @RequestParam(required = false) Double dropLat, @RequestParam(required = false) Double dropLng,
                            @RequestParam VehicleType vehicleType) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");

        if (pickupLat == null || pickupLng == null || dropLat == null || dropLng == null) {
            model.addAttribute("user", currentUser);
            model.addAttribute("vehicleTypes", VehicleType.values());
            model.addAttribute("error", "Please select both a pickup point and a destination on the map.");
            return "rider/dashboard";
        }

        RideRequestDTO req = new RideRequestDTO();
        req.setRiderId(currentUser.getId());
        req.setPickupLat(pickupLat);
        req.setPickupLng(pickupLng);
        req.setDropLat(dropLat);
        req.setDropLng(dropLng);
        req.setVehicleType(vehicleType);

        RideResponseDTO ride = rideService.requestRide(req);
        return "redirect:/rider/ride/" + ride.getId();
    }

    @GetMapping("/ride/{id}")
    public String rideDetail(@PathVariable Long id, Model model) {
        RideResponseDTO ride = rideService.getRide(id);
        model.addAttribute("ride", ride);

        if (ride.getStatus().name().equals("COMPLETED")) {
            try {
                Payment payment = paymentService.getByRideId(id);
                model.addAttribute("payment", payment);
                model.addAttribute("isPaid", payment.getStatus() == PaymentStatus.PAID);
            } catch (Exception ignored) {
                // no payment record yet, shouldn't normally happen
            }
        }
        return "rider/ride-detail";
    }

    @PostMapping("/ride/{id}/cancel")
    public String cancelRide(@PathVariable Long id) {
        rideService.cancelRide(id, null);
        return "redirect:/rider/ride/" + id;
    }

    @PostMapping("/ride/{id}/retry-match")
    public String retryMatch(@PathVariable Long id) {
        rideService.retryMatch(id);
        return "redirect:/rider/ride/" + id;
    }

    @GetMapping("/history")
    public String history(HttpSession session, Model model) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");
        model.addAttribute("rides", rideService.getRidesByRider(currentUser.getId()));
        return "rider/history";
    }
}
