package com.ridelink.driver.service;

import com.ridelink.driver.dto.*;
import com.ridelink.driver.entity.*;
import com.ridelink.driver.exception.BusinessException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DriverService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DriverService Tests")
class DriverServiceTest {

    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private VehicleRepository vehicleRepository;

    @InjectMocks private DriverService driverService;

    private DriverProfile onlineDriver;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicle = Vehicle.builder()
                .id(10L)
                .plateNumber("CAB-1234")
                .make("Toyota")
                .model("Prius")
                .year(2022)
                .color("Silver")
                .vehicleType(VehicleType.SEDAN)
                .capacity(4)
                .build();

        onlineDriver = DriverProfile.builder()
                .id(1L)
                .accountUserId(100L)
                .fullName("Kamal Silva")
                .email("kamal@example.com")
                .licenseNumber("LK-001")
                .serviceArea("Colombo")
                .availabilityStatus(AvailabilityStatus.ONLINE)
                .currentLatitude(6.9271)
                .currentLongitude(79.8612)
                .isVerified(true)
                .vehicle(vehicle)
                .build();

        vehicle.setDriverProfile(onlineDriver);
    }

    @Nested
    @DisplayName("Profile Registration")
    class RegistrationTests {

        @Test
        @DisplayName("Happy Path: Should create driver profile and vehicle")
        void register_happyPath() {
            VehicleRegistrationRequest vr = new VehicleRegistrationRequest();
            vr.setPlateNumber("CAB-5678");
            vr.setMake("Honda");
            vr.setModel("Fit");
            vr.setYear(2021);
            vr.setColor("Blue");
            vr.setVehicleType(VehicleType.HATCHBACK);
            vr.setCapacity(4);

            DriverRegistrationRequest req = new DriverRegistrationRequest();
            req.setLicenseNumber("LK-NEW-001");
            req.setServiceArea("Gampaha");
            req.setVehicle(vr);

            DriverProfile newProfile = DriverProfile.builder()
                    .id(2L).accountUserId(200L).fullName("Nimal").email("nimal@test.com")
                    .licenseNumber("LK-NEW-001").serviceArea("Gampaha")
                    .availabilityStatus(AvailabilityStatus.OFFLINE).isVerified(false)
                    .currentLatitude(0.0).currentLongitude(0.0).build();
            Vehicle newVehicle = Vehicle.builder()
                    .id(20L).plateNumber("CAB-5678").make("Honda").model("Fit")
                    .year(2021).color("Blue").vehicleType(VehicleType.HATCHBACK).capacity(4)
                    .driverProfile(newProfile).build();

            when(driverProfileRepository.existsByAccountUserId(200L)).thenReturn(false);
            when(driverProfileRepository.existsByLicenseNumber("LK-NEW-001")).thenReturn(false);
            when(vehicleRepository.existsByPlateNumber("CAB-5678")).thenReturn(false);
            when(driverProfileRepository.save(any())).thenReturn(newProfile);
            when(vehicleRepository.save(any())).thenReturn(newVehicle);

            DriverProfileDTO result = driverService.registerDriverProfile(200L, "Nimal",
                    "nimal@test.com", req);

            assertThat(result).isNotNull();
            assertThat(result.getLicenseNumber()).isEqualTo("LK-NEW-001");
            assertThat(result.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
        }

        @Test
        @DisplayName("Failure: Duplicate driver profile")
        void register_duplicateProfile_throws() {
            DriverRegistrationRequest req = new DriverRegistrationRequest();
            req.setLicenseNumber("LK-001"); req.setServiceArea("Colombo");
            req.setVehicle(new VehicleRegistrationRequest());

            when(driverProfileRepository.existsByAccountUserId(100L)).thenReturn(true);

            assertThatThrownBy(() -> driverService.registerDriverProfile(100L, "K", "k@k.com", req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("Availability Toggle")
    class AvailabilityTests {

        @Test
        @DisplayName("Happy Path: ONLINE → OFFLINE")
        void toggle_onlineToOffline() {
            when(driverProfileRepository.findByAccountUserId(100L)).thenReturn(Optional.of(onlineDriver));
            when(driverProfileRepository.save(any())).thenReturn(onlineDriver);

            DriverProfileDTO result = driverService.toggleAvailability(100L);

            assertThat(onlineDriver.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
        }

        @Test
        @DisplayName("Failure: Cannot toggle while ON_TRIP")
        void toggle_onTrip_throws() {
            onlineDriver.setAvailabilityStatus(AvailabilityStatus.ON_TRIP);
            when(driverProfileRepository.findByAccountUserId(100L)).thenReturn(Optional.of(onlineDriver));

            assertThatThrownBy(() -> driverService.toggleAvailability(100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("on a trip");
        }
    }

    @Nested
    @DisplayName("Nearby Driver Search")
    class NearbyTests {

        @Test
        @DisplayName("Happy Path: Returns available drivers sorted by distance")
        void getAvailableDrivers_returnsResults() {
            when(driverProfileRepository.findAvailableDriversNearby(
                    anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(onlineDriver));

            List<AvailableDriverDTO> result = driverService.getAvailableDriversNearby(
                    6.9271, 79.8612, 10.0);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFullName()).isEqualTo("Kamal Silva");
            assertThat(result.get(0).getDistanceKm()).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("Edge Case: Returns empty list when no drivers nearby")
        void getAvailableDrivers_noDrivers_returnsEmpty() {
            when(driverProfileRepository.findAvailableDriversNearby(
                    anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of());

            List<AvailableDriverDTO> result = driverService.getAvailableDriversNearby(
                    0.0, 0.0, 5.0);

            assertThat(result).isEmpty();
        }
    }
}
