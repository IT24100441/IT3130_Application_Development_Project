package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.*;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.entity.RideStatus;
import com.ridelink.ride.exception.*;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RideService state machine and workflows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RideService Tests")
class RideServiceTest {

    @Mock private RideRepository rideRepository;
    @Mock private DriverServiceClient driverServiceClient;
    @Mock private FareServiceClient fareServiceClient;

    @InjectMocks private RideService rideService;

    private Ride requestedRide;
    private Ride assignedRide;

    @BeforeEach
    void setUp() {
        requestedRide = Ride.builder()
                .id(1L).passengerAccountId(10L)
                .pickupAddress("Colombo 03").pickupLatitude(6.9271).pickupLongitude(79.8612)
                .destinationAddress("Airport").destinationLatitude(7.1800).destinationLongitude(79.8845)
                .estimatedDistanceKm(35.0).estimatedFare(new BigDecimal("3150.00"))
                .status(RideStatus.REQUESTED).requestedAt(LocalDateTime.now())
                .build();

        assignedRide = Ride.builder()
                .id(2L).passengerAccountId(10L)
                .driverAccountId(20L).driverProfileId(5L)
                .assignedDriverName("Kamal").assignedVehiclePlate("CAB-1234")
                .pickupAddress("Colombo 03").pickupLatitude(6.9271).pickupLongitude(79.8612)
                .destinationAddress("Airport").destinationLatitude(7.1800).destinationLongitude(79.8845)
                .estimatedDistanceKm(35.0).estimatedFare(new BigDecimal("3150.00"))
                .status(RideStatus.ASSIGNED).requestedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Create Ride")
    class CreateRideTests {

        @Test
        @DisplayName("Happy Path: Creates ride and assigns nearest driver")
        void createRide_withAvailableDriver_assignsDriver() {
            RideRequestDTO req = new RideRequestDTO();
            req.setPickupAddress("Colombo 03"); req.setPickupLatitude(6.9271); req.setPickupLongitude(79.8612);
            req.setDestinationAddress("Airport"); req.setDestinationLatitude(7.1800); req.setDestinationLongitude(79.8845);

            AvailableDriverDTO driver = AvailableDriverDTO.builder()
                    .driverProfileId(5L).accountUserId(20L)
                    .fullName("Kamal").plateNumber("CAB-1234").distanceKm(2.0).build();

            when(rideRepository.findByPassengerAccountIdAndStatus(anyLong(), any())).thenReturn(List.of());
            when(rideRepository.save(any())).thenReturn(requestedRide);
            when(driverServiceClient.getAvailableDrivers(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(driver));

            // Second save returns assignedRide
            when(rideRepository.save(any())).thenReturn(assignedRide);

            RideDTO result = rideService.createRide(10L, req);

            assertThat(result).isNotNull();
            verify(driverServiceClient).getAvailableDrivers(anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("Edge Case: Ride stays REQUESTED when no drivers available")
        void createRide_noDrivers_staysRequested() {
            RideRequestDTO req = new RideRequestDTO();
            req.setPickupAddress("Remote Area"); req.setPickupLatitude(8.0); req.setPickupLongitude(80.0);
            req.setDestinationAddress("City"); req.setDestinationLatitude(6.9); req.setDestinationLongitude(79.8);

            when(rideRepository.findByPassengerAccountIdAndStatus(anyLong(), any())).thenReturn(List.of());
            when(rideRepository.save(any())).thenReturn(requestedRide);
            when(driverServiceClient.getAvailableDrivers(anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of());

            RideDTO result = rideService.createRide(10L, req);

            assertThat(result).isNotNull();
            // No driver assigned → status REQUESTED
        }

        @Test
        @DisplayName("Failure: Passenger already has an active ride")
        void createRide_existingActiveRide_throws() {
            when(rideRepository.findByPassengerAccountIdAndStatus(10L, RideStatus.REQUESTED))
                    .thenReturn(List.of(requestedRide));
            when(rideRepository.findByPassengerAccountIdAndStatus(10L, RideStatus.ASSIGNED))
                    .thenReturn(List.of());
            when(rideRepository.findByPassengerAccountIdAndStatus(10L, RideStatus.ACCEPTED))
                    .thenReturn(List.of());
            when(rideRepository.findByPassengerAccountIdAndStatus(10L, RideStatus.IN_PROGRESS))
                    .thenReturn(List.of());

            RideRequestDTO req = new RideRequestDTO();
            req.setPickupAddress("A"); req.setPickupLatitude(1.0); req.setPickupLongitude(1.0);
            req.setDestinationAddress("B"); req.setDestinationLatitude(2.0); req.setDestinationLongitude(2.0);

            assertThatThrownBy(() -> rideService.createRide(10L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("active ride");
        }
    }

    @Nested
    @DisplayName("State Machine Transitions")
    class StateMachineTests {

        @Test
        @DisplayName("Happy Path: ASSIGNED → ACCEPTED by assigned driver")
        void transition_assignedToAccepted_byDriver() {
            when(rideRepository.findById(2L)).thenReturn(Optional.of(assignedRide));
            when(rideRepository.save(any())).thenReturn(assignedRide);

            RideDTO result = rideService.updateRideStatus(2L, RideStatus.ACCEPTED, 20L, true, null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Failure: Invalid transition REQUESTED → COMPLETED")
        void transition_invalidSkip_throws() {
            when(rideRepository.findById(1L)).thenReturn(Optional.of(requestedRide));

            assertThatThrownBy(() -> rideService.updateRideStatus(1L, RideStatus.COMPLETED, 20L, true, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("Failure: Transition on COMPLETED ride throws")
        void transition_fromCompletedState_throws() {
            requestedRide.setStatus(RideStatus.COMPLETED);
            when(rideRepository.findById(1L)).thenReturn(Optional.of(requestedRide));

            assertThatThrownBy(() -> rideService.updateRideStatus(1L, RideStatus.CANCELLED, 10L, false, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("terminal state");
        }

        @Test
        @DisplayName("Failure: Wrong driver trying to accept")
        void transition_wrongDriver_throws() {
            when(rideRepository.findById(2L)).thenReturn(Optional.of(assignedRide));

            // Driver ID 99 is NOT the assigned driver (20L)
            assertThatThrownBy(() -> rideService.updateRideStatus(2L, RideStatus.ACCEPTED, 99L, true, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("assigned driver");
        }

        @Test
        @DisplayName("Happy Path: CANCELLED with reason by passenger")
        void cancel_byPassenger_success() {
            when(rideRepository.findById(2L)).thenReturn(Optional.of(assignedRide));
            when(rideRepository.save(any())).thenReturn(assignedRide);

            RideStatusUpdateRequest req = new RideStatusUpdateRequest();
            req.setCancellationReason("Changed plans");

            RideDTO result = rideService.updateRideStatus(2L, RideStatus.CANCELLED, 10L, false, req);

            assertThat(result).isNotNull();
            verify(driverServiceClient).updateDriverStatus(5L, "ONLINE");
        }
    }
}
