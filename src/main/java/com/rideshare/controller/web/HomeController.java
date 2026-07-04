package com.rideshare.controller.web;

import com.rideshare.dto.UserResponseDTO;
import com.rideshare.entity.Role;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpSession session) {
        UserResponseDTO currentUser = (UserResponseDTO) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        return currentUser.getRole() == Role.RIDER ? "redirect:/rider/dashboard" : "redirect:/driver/dashboard";
    }
}
