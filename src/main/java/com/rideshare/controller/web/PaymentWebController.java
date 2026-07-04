package com.rideshare.controller.web;

import com.rideshare.entity.Payment;
import com.rideshare.entity.PaymentMethod;
import com.rideshare.service.PaymentService;
import com.rideshare.service.RideService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payment")
public class PaymentWebController {

    private final PaymentService paymentService;
    private final RideService rideService;

    public PaymentWebController(PaymentService paymentService, RideService rideService) {
        this.paymentService = paymentService;
        this.rideService = rideService;
    }

    @GetMapping("/{rideId}")
    public String payPage(@PathVariable Long rideId, Model model) {
        Payment payment = paymentService.getByRideId(rideId);
        if (payment.getStatus() == com.rideshare.entity.PaymentStatus.PAID) {
            return "redirect:/payment/" + rideId + "/receipt";
        }
        model.addAttribute("payment", payment);
        model.addAttribute("ride", rideService.getRide(rideId));
        model.addAttribute("methods", PaymentMethod.values());
        return "payment/pay";
    }

    @PostMapping("/{rideId}")
    public String processPayment(@PathVariable Long rideId, @RequestParam PaymentMethod method) {
        paymentService.processPayment(rideId, method);
        return "redirect:/payment/" + rideId + "/receipt";
    }

    @GetMapping("/{rideId}/receipt")
    public String receipt(@PathVariable Long rideId, Model model) {
        model.addAttribute("payment", paymentService.getByRideId(rideId));
        model.addAttribute("ride", rideService.getRide(rideId));
        return "payment/receipt";
    }
}
