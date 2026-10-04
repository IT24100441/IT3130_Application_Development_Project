package com.ridelink.ride.repository;

import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.entity.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    Page<Ride> findByPassengerAccountIdOrderByCreatedAtDesc(Long passengerAccountId, Pageable pageable);

    Page<Ride> findByDriverAccountIdOrderByCreatedAtDesc(Long driverAccountId, Pageable pageable);

    List<Ride> findByPassengerAccountIdAndStatus(Long passengerAccountId, RideStatus status);

    boolean existsByDriverProfileIdAndStatusIn(Long driverProfileId, List<RideStatus> statuses);
}
