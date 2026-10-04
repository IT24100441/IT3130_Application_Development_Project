package com.ridelink.driver.repository;

import com.ridelink.driver.entity.AvailabilityStatus;
import com.ridelink.driver.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Driver profile repository.
 * The Haversine formula is used to find drivers within a radius (km) from a point.
 */
@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    Optional<DriverProfile> findByAccountUserId(Long accountUserId);

    boolean existsByAccountUserId(Long accountUserId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<DriverProfile> findByAvailabilityStatus(AvailabilityStatus status);

    /**
     * Finds ONLINE drivers within a given radius using the Haversine formula.
     * Note: This is approximate and suitable for demonstration purposes.
     * Production systems should use a geospatial index (PostGIS, MongoDB 2dsphere).
     *
     * @param lat    search center latitude
     * @param lng    search center longitude
     * @param radius search radius in kilometres
     */
    @Query(value = """
            SELECT d.* FROM driver_profiles d
            WHERE d.availability_status = 'ONLINE'
              AND d.is_verified = TRUE
              AND (6371 * acos(
                    cos(radians(:lat)) * cos(radians(d.current_latitude))
                    * cos(radians(d.current_longitude) - radians(:lng))
                    + sin(radians(:lat)) * sin(radians(d.current_latitude))
                  )) <= :radius
            ORDER BY (6371 * acos(
                    cos(radians(:lat)) * cos(radians(d.current_latitude))
                    * cos(radians(d.current_longitude) - radians(:lng))
                    + sin(radians(:lat)) * sin(radians(d.current_latitude))
                  )) ASC
            LIMIT 10
            """, nativeQuery = true)
    List<DriverProfile> findAvailableDriversNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radius
    );
}
