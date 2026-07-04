package com.rideshare.service;

import com.rideshare.entity.*;
import com.rideshare.exception.InvalidRideStateException;
import com.rideshare.exception.ResourceNotFoundException;
import com.rideshare.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simulated payment processing - no real payment gateway involved.
 * A Payment record is created (PENDING) automatically when a ride completes,
 * and "processed" here by simply marking it PAID with a fake transaction ref.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment createPendingPayment(Ride ride) {
        Payment payment = new Payment();
        payment.setRide(ride);
        payment.setAmount(ride.getFare());
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    public Payment getByRideId(Long rideId) {
        return paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for ride " + rideId));
    }

    @Transactional
    public Payment processPayment(Long rideId, PaymentMethod method) {
        Payment payment = getByRideId(rideId);
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new InvalidRideStateException("Ride " + rideId + " is already paid");
        }

        // Simulated processing - always "succeeds" for demo purposes.
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }
}
