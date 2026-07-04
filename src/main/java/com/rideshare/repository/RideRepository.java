package com.rideshare.repository;

import com.rideshare.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByRiderIdOrderByRequestedAtDesc(Long riderId);

    List<Ride> findByDriverIdOrderByRequestedAtDesc(Long driverId);
}
