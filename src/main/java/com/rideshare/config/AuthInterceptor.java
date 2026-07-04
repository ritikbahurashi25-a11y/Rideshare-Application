package com.rideshare.config;

import com.rideshare.dto.UserResponseDTO;
import com.rideshare.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards the Thymeleaf web pages (not the /api/** REST endpoints).
 * Requires a "currentUser" object in the session for /rider/**, /driver/**,
 * and /payment/** paths, and enforces that riders only access /rider/**
 * and drivers only access /driver/**.
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        HttpSession session = request.getSession(false);
        UserResponseDTO currentUser = (session != null) ? (UserResponseDTO) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect("/login");
            return false;
        }

        if (path.startsWith("/rider") && currentUser.getRole() != Role.RIDER) {
            response.sendRedirect("/driver/dashboard");
            return false;
        }

        if (path.startsWith("/driver") && currentUser.getRole() != Role.DRIVER) {
            response.sendRedirect("/rider/dashboard");
            return false;
        }

        return true;
    }
}
