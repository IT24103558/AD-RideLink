package com.ridelink.rideservice.service;

import com.ridelink.rideservice.client.DriverClient;
import com.ridelink.rideservice.client.DriverInfo;
import com.ridelink.rideservice.client.FareClient;
import com.ridelink.rideservice.client.FareInfo;
import com.ridelink.rideservice.dto.CancelRideRequest;
import com.ridelink.rideservice.dto.CompleteRideRequest;
import com.ridelink.rideservice.dto.CreateRideRequest;
import com.ridelink.rideservice.dto.PlaceDto;
import com.ridelink.rideservice.dto.RideResponse;
import com.ridelink.rideservice.exception.ApiException;
import com.ridelink.rideservice.model.Place;
import com.ridelink.rideservice.model.Ride;
import com.ridelink.rideservice.model.RideStatus;
import com.ridelink.rideservice.repository.RideRepository;
import com.ridelink.rideservice.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;
    @Mock
    private DriverClient driverClient;
    @Mock
    private FareClient fareClient;

    private RideService rideService;

    private final CurrentUser passenger = new CurrentUser("pass-1", "PASSENGER", "pass-token");
    private final CurrentUser driver1 = new CurrentUser("drv-user-1", "DRIVER", "drv1-token");
    private final CurrentUser driver2 = new CurrentUser("drv-user-2", "DRIVER", "drv2-token");
    private final CurrentUser admin = new CurrentUser("adm-1", "ADMIN", "adm-token");

    @BeforeEach
    void setUp() {
        rideService = new RideService(rideRepository, driverClient, fareClient);
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId("ride-1");
            }
            return r;
        });
        when(rideRepository.existsByPassengerIdAndStatusIn(anyString(), any())).thenReturn(false);
        when(rideRepository.findByStatusIn(any())).thenReturn(List.of());
        when(fareClient.estimate(anyString(), any(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble())).thenReturn(Optional.empty());
    }

    private CreateRideRequest createRequest(String vehicleType) {
        return new CreateRideRequest(
                new PlaceDto("Fort", 6.9271, 79.8612),
                new PlaceDto("Mount Lavinia", 6.8389, 79.8653),
                vehicleType, null);
    }

    private DriverInfo driverInfo(String profileId, String userId, String type, Double lat, Double lng) {
        return new DriverInfo(profileId, userId, "Name " + profileId, "0771234567",
                new DriverInfo.VehicleInfo("CAB-" + profileId, type, "Prius", 4), "AVAILABLE", "Colombo",
                lat == null ? null : new DriverInfo.LocationInfo(lat, lng));
    }

    private Ride ride(RideStatus status, String driverUserId) {
        Ride r = new Ride();
        r.setId("ride-1");
        r.setPassengerId("pass-1");
        r.setPickup(new Place("Fort", 6.9271, 79.8612));
        r.setDestination(new Place("Mount Lavinia", 6.8389, 79.8653));
        r.setStatus(status);
        r.setDriverProfileId(driverUserId == null ? null : "prof-1");
        r.setDriverUserId(driverUserId);
        r.setVehicleType("CAR");
        r.setEstimatedDistanceKm(12.0);
        r.setEstimatedDurationMinutes(24.0);
        return r;
    }

    // ---------------------------------------------------------------- create

    @Test
    void create_assigns_the_nearest_driver() {
        when(driverClient.getAvailableDrivers(anyString(), any())).thenReturn(List.of(
                driverInfo("far", "u-far", "CAR", 7.2906, 80.6337),
                driverInfo("near", "u-near", "CAR", 6.93, 79.86),
                driverInfo("noloc", "u-noloc", "CAR", null, null)));

        RideResponse res = rideService.create(createRequest(null), passenger);

        assertThat(res.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(res.driverProfileId()).isEqualTo("near");
        assertThat(res.driverUserId()).isEqualTo("u-near");
        assertThat(res.vehicleType()).isEqualTo("CAR");
        assertThat(res.estimatedDistanceKm()).isGreaterThan(0);
        assertThat(res.passengerId()).isEqualTo("pass-1");
    }

    @Test
    void create_uses_the_fare_estimate_when_available() {
        when(fareClient.estimate(anyString(), any(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(Optional.of(new FareInfo(null, new BigDecimal("1500.00"), "LKR", 14.5, 29.0)));
        when(driverClient.getAvailableDrivers(anyString(), any())).thenReturn(List.of());

        RideResponse res = rideService.create(createRequest("CAR"), passenger);

        assertThat(res.estimatedFare()).isEqualByComparingTo("1500.00");
        assertThat(res.currency()).isEqualTo("LKR");
        assertThat(res.estimatedDistanceKm()).isEqualTo(14.5);
        assertThat(res.status()).isEqualTo(RideStatus.REQUESTED);
    }

    @Test
    void create_skips_drivers_who_already_have_an_active_ride() {
        when(driverClient.getAvailableDrivers(anyString(), any())).thenReturn(List.of(
                driverInfo("near", "u-near", "CAR", 6.93, 79.86),
                driverInfo("other", "u-other", "CAR", 7.2906, 80.6337)));
        Ride busy = ride(RideStatus.IN_PROGRESS, "u-near");
        busy.setDriverProfileId("near");
        when(rideRepository.findByStatusIn(any())).thenReturn(List.of(busy));

        RideResponse res = rideService.create(createRequest(null), passenger);

        assertThat(res.driverProfileId()).isEqualTo("other");
    }

    @Test
    void create_respects_the_requested_vehicle_type() {
        when(driverClient.getAvailableDrivers(anyString(), any())).thenReturn(List.of(
                driverInfo("car", "u-car", "CAR", 6.93, 79.86),
                driverInfo("tuk", "u-tuk", "TUK", 7.2906, 80.6337)));

        RideResponse res = rideService.create(createRequest("tuk"), passenger);

        assertThat(res.driverProfileId()).isEqualTo("tuk");
    }

    @Test
    void create_without_any_driver_leaves_the_ride_requested() {
        when(driverClient.getAvailableDrivers(anyString(), any())).thenReturn(List.of());

        RideResponse res = rideService.create(createRequest(null), passenger);

        assertThat(res.status()).isEqualTo(RideStatus.REQUESTED);
        assertThat(res.driverUserId()).isNull();
    }

    @Test
    void create_still_works_when_the_driver_service_is_down() {
        when(driverClient.getAvailableDrivers(anyString(), any())).thenThrow(
                new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "DRIVER_SERVICE_UNAVAILABLE", "down"));

        RideResponse res = rideService.create(createRequest(null), passenger);

        assertThat(res.status()).isEqualTo(RideStatus.REQUESTED);
    }

    @Test
    void create_is_refused_while_the_passenger_has_an_active_ride() {
        when(rideRepository.existsByPassengerIdAndStatusIn(eq("pass-1"), any())).thenReturn(true);

        assertThatThrownBy(() -> rideService.create(createRequest(null), passenger))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("active ride");
    }

    @Test
    void create_rejects_identical_pickup_and_destination() {
        CreateRideRequest same = new CreateRideRequest(new PlaceDto("A", 6.9, 79.8), new PlaceDto("A", 6.9, 79.8), null, null);
        assertThatThrownBy(() -> rideService.create(same, passenger))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("50 metres");
    }

    // ---------------------------------------------------------------- assign

    @Test
    void assign_retry_succeeds_once_a_driver_is_free() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.REQUESTED, null)));
        when(driverClient.getAvailableDrivers(anyString(), any()))
                .thenReturn(List.of(driverInfo("p1", "drv-user-1", "CAR", 6.93, 79.86)));

        RideResponse res = rideService.assign("ride-1", passenger);

        assertThat(res.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(res.driverUserId()).isEqualTo("drv-user-1");
    }

    @Test
    void assign_without_driver_is_404() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.REQUESTED, null)));
        when(driverClient.getAvailableDrivers(anyString(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> rideService.assign("ride-1", passenger))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No available driver");
    }

    @Test
    void assign_on_an_already_assigned_ride_is_a_conflict() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.ASSIGNED, "drv-user-1")));
        assertThatThrownBy(() -> rideService.assign("ride-1", passenger)).isInstanceOf(ApiException.class);
    }

    // ---------------------------------------------------------------- accept / reject / start

    @Test
    void assigned_driver_can_accept() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.ASSIGNED, "drv-user-1")));

        RideResponse res = rideService.accept("ride-1", driver1);

        assertThat(res.status()).isEqualTo(RideStatus.ACCEPTED);
        assertThat(res.acceptedAt()).isNotNull();
    }

    @Test
    void another_driver_cannot_accept() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.ASSIGNED, "drv-user-1")));

        assertThatThrownBy(() -> rideService.accept("ride-1", driver2))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not assigned to you");
    }

    @Test
    void accept_in_the_wrong_state_is_a_conflict() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.REQUESTED, "drv-user-1")));

        assertThatThrownBy(() -> rideService.accept("ride-1", driver1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("requires it to be ASSIGNED");
    }

    @Test
    void reject_returns_the_ride_to_the_pool_and_never_reassigns_the_same_driver() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.ASSIGNED, "drv-user-1")));
        when(driverClient.getAvailableDrivers(anyString(), any())).thenReturn(List.of(
                driverInfo("prof-1", "drv-user-1", "CAR", 6.93, 79.86)));

        RideResponse res = rideService.reject("ride-1", driver1);

        assertThat(res.status()).isEqualTo(RideStatus.REQUESTED);
        assertThat(res.driverUserId()).isNull();
    }

    @Test
    void reject_reassigns_to_another_driver_when_one_is_free() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.ASSIGNED, "drv-user-1")));
        when(driverClient.getAvailableDrivers(anyString(), any())).thenReturn(List.of(
                driverInfo("prof-1", "drv-user-1", "CAR", 6.93, 79.86),
                driverInfo("prof-2", "drv-user-2", "CAR", 7.0, 79.9)));

        RideResponse res = rideService.reject("ride-1", driver1);

        assertThat(res.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(res.driverUserId()).isEqualTo("drv-user-2");
    }

    @Test
    void start_marks_the_driver_busy() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.ACCEPTED, "drv-user-1")));

        RideResponse res = rideService.start("ride-1", driver1);

        assertThat(res.status()).isEqualTo(RideStatus.IN_PROGRESS);
        assertThat(res.startedAt()).isNotNull();
        verify(driverClient).setAvailability("drv1-token", "prof-1", "BUSY");
    }

    @Test
    void cannot_start_a_ride_that_was_not_accepted() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.ASSIGNED, "drv-user-1")));
        assertThatThrownBy(() -> rideService.start("ride-1", driver1)).isInstanceOf(ApiException.class);
    }

    // ---------------------------------------------------------------- complete

    @Test
    void complete_calculates_the_final_fare_and_frees_the_driver() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.IN_PROGRESS, "drv-user-1")));
        when(fareClient.createFinalFare(anyString(), any())).thenReturn(
                new FareInfo("fare-1", new BigDecimal("1560.00"), "LKR", 12.0, 24.0));

        RideResponse res = rideService.complete("ride-1", null, driver1);

        assertThat(res.status()).isEqualTo(RideStatus.COMPLETED);
        assertThat(res.finalFare()).isEqualByComparingTo("1560.00");
        assertThat(res.fareId()).isEqualTo("fare-1");
        assertThat(res.fareStatus()).isEqualTo("CALCULATED");
        assertThat(res.actualDistanceKm()).isEqualTo(12.0);
        verify(driverClient).setAvailability("drv1-token", "prof-1", "AVAILABLE");
    }

    @Test
    void complete_uses_the_actual_values_when_given() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.IN_PROGRESS, "drv-user-1")));
        when(fareClient.createFinalFare(anyString(), any())).thenReturn(
                new FareInfo("fare-1", new BigDecimal("2000.00"), "LKR", 15.0, 30.0));

        RideResponse res = rideService.complete("ride-1", new CompleteRideRequest(15.0, 30.0), driver1);

        assertThat(res.actualDistanceKm()).isEqualTo(15.0);
        assertThat(res.actualDurationMinutes()).isEqualTo(30.0);
        verify(fareClient).createFinalFare(eq("drv1-token"),
                eq(new FareClient.FinalFareBody("ride-1", "pass-1", "drv-user-1", "CAR", 15.0, 30.0)));
    }

    @Test
    void complete_still_succeeds_when_the_fare_service_is_down_and_fare_stays_pending() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.IN_PROGRESS, "drv-user-1")));
        when(fareClient.createFinalFare(anyString(), any())).thenThrow(
                new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "FARE_SERVICE_UNAVAILABLE", "down"));

        RideResponse res = rideService.complete("ride-1", null, driver1);

        assertThat(res.status()).isEqualTo(RideStatus.COMPLETED);
        assertThat(res.fareStatus()).isEqualTo("PENDING");
        assertThat(res.finalFare()).isNull();
    }

    @Test
    void sync_fare_completes_a_pending_fare() {
        Ride done = ride(RideStatus.COMPLETED, "drv-user-1");
        done.setFareStatus("PENDING");
        done.setActualDistanceKm(12.0);
        done.setActualDurationMinutes(24.0);
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(done));
        when(fareClient.createFinalFare(anyString(), any())).thenReturn(
                new FareInfo("fare-9", new BigDecimal("1560.00"), "LKR", 12.0, 24.0));

        RideResponse res = rideService.syncFare("ride-1", driver1);

        assertThat(res.fareStatus()).isEqualTo("CALCULATED");
        assertThat(res.fareId()).isEqualTo("fare-9");
    }

    // ---------------------------------------------------------------- cancel

    @Test
    void passenger_can_cancel_before_the_trip_starts() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.ACCEPTED, "drv-user-1")));

        RideResponse res = rideService.cancel("ride-1", new CancelRideRequest("changed my mind"), passenger);

        assertThat(res.status()).isEqualTo(RideStatus.CANCELLED);
        assertThat(res.cancelReason()).isEqualTo("changed my mind");
        assertThat(res.cancelledByRole()).isEqualTo("PASSENGER");
    }

    @Test
    void cancel_without_a_body_is_allowed() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.REQUESTED, null)));
        assertThat(rideService.cancel("ride-1", null, passenger).status()).isEqualTo(RideStatus.CANCELLED);
    }

    @Test
    void cannot_cancel_a_trip_in_progress() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.IN_PROGRESS, "drv-user-1")));

        assertThatThrownBy(() -> rideService.cancel("ride-1", null, passenger))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    @Test
    void a_stranger_cannot_cancel() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.REQUESTED, null)));
        CurrentUser stranger = new CurrentUser("someone-else", "PASSENGER", "t");
        assertThatThrownBy(() -> rideService.cancel("ride-1", null, stranger)).isInstanceOf(ApiException.class);
    }

    // ---------------------------------------------------------------- queries

    @Test
    void get_is_limited_to_the_parties_and_admin() {
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride(RideStatus.ASSIGNED, "drv-user-1")));

        assertThat(rideService.get("ride-1", passenger).id()).isEqualTo("ride-1");
        assertThat(rideService.get("ride-1", driver1).id()).isEqualTo("ride-1");
        assertThat(rideService.get("ride-1", admin).id()).isEqualTo("ride-1");
        assertThatThrownBy(() -> rideService.get("ride-1", driver2)).isInstanceOf(ApiException.class);
    }

    @Test
    void get_unknown_ride_is_404() {
        when(rideRepository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> rideService.get("nope", admin))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Ride not found");
    }

    @Test
    void mine_returns_the_rides_for_the_callers_role_and_filters_by_status() {
        Ride a = ride(RideStatus.COMPLETED, "drv-user-1");
        Ride b = ride(RideStatus.CANCELLED, "drv-user-1");
        when(rideRepository.findByPassengerIdOrderByRequestedAtDesc("pass-1")).thenReturn(List.of(a, b));
        when(rideRepository.findByDriverUserIdOrderByRequestedAtDesc("drv-user-1")).thenReturn(List.of(a));

        assertThat(rideService.mine(passenger, null)).hasSize(2);
        assertThat(rideService.mine(passenger, RideStatus.CANCELLED)).hasSize(1);
        assertThat(rideService.mine(driver1, null)).hasSize(1);
    }
}
