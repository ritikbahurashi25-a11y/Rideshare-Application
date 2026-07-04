package com.rideshare.controller.web;

import com.rideshare.dto.AvailabilityRequest;
import com.rideshare.dto.RideResponseDTO;
import com.rideshare.dto.UserResponseDTO;
import com.rideshare.service.RideService;
import com.rideshare.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/driver")
public class DriverWebController {

    private final RideService rideService;
    private final UserService userService;

    public DriverWebController(RideService rideService, UserService userService) {
        this.rideService = rideService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        UserResponseDTO sessionUser = (UserResponseDTO) session.getAttribute("currentUser");
        // fetch fresh copy so availability/location reflects latest state
        UserResponseDTO currentUser = userService.getUser(sessionUser.getId());
        session.setAttribute("currentUser", currentUser);

        model.addAttribute("user", currentUser);
        Optional<RideResponseDTO> activeRide = rideService.getActiveRideForDriver(currentUser.getId());
        model.addAttribute("activeRide", activeRide.orElse(null));
        return "driver/dashboard";
    }

    @PostMapping("/availability")
    public String toggleAvailability(HttpSession session, @RequestParam boolean available) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");
        AvailabilityRequest req = new AvailabilityRequest();
        req.setAvailable(available);
        UserResponseDTO updated = userService.setAvailability(currentUser.getId(), req);
        session.setAttribute("currentUser", updated);
        return "redirect:/driver/dashboard";
    }

    @GetMapping("/ride/{id}")
    public String rideDetail(@PathVariable Long id, Model model) {
        model.addAttribute("ride", rideService.getRide(id));
        return "driver/ride-detail";
    }

    @PostMapping("/ride/{id}/accept")
    public String accept(@PathVariable Long id, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");
        rideService.acceptRide(id, currentUser.getId());
        return "redirect:/driver/ride/" + id;
    }

    @PostMapping("/ride/{id}/reject")
    public String reject(@PathVariable Long id, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");
        rideService.rejectRide(id, currentUser.getId());
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/ride/{id}/start")
    public String start(@PathVariable Long id, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");
        rideService.startRide(id, currentUser.getId());
        return "redirect:/driver/ride/" + id;
    }

    @PostMapping("/ride/{id}/complete")
    public String complete(@PathVariable Long id, HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");
        rideService.completeRide(id, currentUser.getId());
        return "redirect:/driver/ride/" + id;
    }

    @GetMapping("/history")
    public String history(HttpSession session, Model model) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");
        model.addAttribute("rides", rideService.getRidesByDriver(currentUser.getId()));
        return "driver/history";
    }
}
