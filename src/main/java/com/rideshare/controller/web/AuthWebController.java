package com.rideshare.controller.web;

import com.rideshare.dto.LoginRequest;
import com.rideshare.dto.RegisterRequest;
import com.rideshare.dto.UserResponseDTO;
import com.rideshare.entity.Role;
import com.rideshare.entity.VehicleType;
import com.rideshare.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthWebController {

    private final UserService userService;

    public AuthWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                         HttpSession session, Model model) {
        try {
            LoginRequest req = new LoginRequest();
            req.setEmail(email);
            req.setPassword(password);
            UserResponseDTO user = userService.login(req);
            session.setAttribute("currentUser", user);
            return user.getRole() == Role.RIDER ? "redirect:/rider/dashboard" : "redirect:/driver/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid email or password");
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("vehicleTypes", VehicleType.values());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest req, Model model) {
        try {
            userService.register(req);
            model.addAttribute("success", "Account created! Please log in.");
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("vehicleTypes", VehicleType.values());
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
