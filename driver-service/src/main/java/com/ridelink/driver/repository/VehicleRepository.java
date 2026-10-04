package com.ridelink.driver.repository;

import com.ridelink.driver.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByDriverProfileId(Long driverProfileId);

    boolean existsByPlateNumber(String plateNumber);
}
