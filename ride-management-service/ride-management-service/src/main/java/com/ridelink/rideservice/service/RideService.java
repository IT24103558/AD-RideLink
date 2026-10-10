package com.ridelink.rideservice.service;

import com.ridelink.rideservice.client.DriverClient;
import com.ridelink.rideservice.client.DriverInfo;
import com.ridelink.rideservice.client.FareClient;
import com.ridelink.rideservice.client.FareInfo;
import com.ridelink.rideservice.dto.CancelRideRequest;
import com.ridelink.rideservice.dto.CompleteRideRequest;
import com.ridelink.rideservice.dto.CreateRideRequest;
import com.ridelink.rideservice.dto.RideResponse;
import com.ridelink.rideservice.exception.ApiException;
import com.ridelink.rideservice.model.Place;
import com.ridelink.rideservice.model.Ride;
import com.ridelink.rideservice.model.RideStatus;
import com.ridelink.rideservice.repository.RideRepository;
import com.ridelink.rideservice.security.CurrentUser;
import com.ridelink.rideservice.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    public static final String FARE_PENDING = "PENDING";
    public static final String FARE_CALCULATED = "CALCULATED";

    private final RideRepository rideRepository;
    private final DriverClient driverClient;
    private final FareClient fareClient;

    public RideService(RideRepository rideRepository, DriverClient driverClient, FareClient fareClient) {
        this.rideRepository = rideRepository;
        this.driverClient = driverClient;
        this.fareClient = fareClient;
    }

    // ------------------------------------------------------------------ create + assign

    public RideResponse create(CreateRideRequest req, CurrentUser caller) {
        if (rideRepository.existsByPassengerIdAndStatusIn(caller.userId(), RideStatus.ACTIVE)) {
            throw ApiException.conflict("ACTIVE_RIDE_EXISTS",
                    "You already have an active ride - complete or cancel it before requesting a new one");
        }
        Place pickup = req.pickup().toPlace();
        Place destination = req.destination().toPlace();
        double straightKm = GeoUtil.haversineKm(pickup.getLatitude(), pickup.getLongitude(),
                destination.getLatitude(), destination.getLongitude());
        if (straightKm < 0.05) {
            throw ApiException.badRequest("INVALID_ROUTE", "Pickup and destination must be at least 50 metres apart");
        }

        Instant now = Instant.now();
        Ride ride = new Ride();
        ride.setPassengerId(caller.userId());
        ride.setPickup(pickup);
        ride.setDestination(destination);
        ride.setRequestedVehicleType(blankToNull(req.vehicleType()));
        ride.setServiceArea(blankToNull(req.serviceArea()));
        ride.setStatus(RideStatus.REQUESTED);
        ride.setRequestedAt(now);
        ride.setUpdatedAt(now);

        // local estimate first (same formula as the Fare Service), overwritten by the Fare Service answer
        double km = GeoUtil.roadDistanceKm(pickup.getLatitude(), pickup.getLongitude(),
                destination.getLatitude(), destination.getLongitude());
        ride.setEstimatedDistanceKm(km);
        ride.setEstimatedDurationMinutes(GeoUtil.durationMinutes(km));
        Optional<FareInfo> estimate = fareClient.estimate(caller.token(), ride.getRequestedVehicleType(),
                pickup.getLatitude(), pickup.getLongitude(), destination.getLatitude(), destination.getLongitude());
        estimate.ifPresent(e -> {
            ride.setEstimatedFare(e.amount());
            ride.setCurrency(e.currency());
            if (e.distanceKm() != null) {
                ride.setEstimatedDistanceKm(e.distanceKm());
            }
            if (e.durationMinutes() != null) {
                ride.setEstimatedDurationMinutes(e.durationMinutes());
            }
        });

        Ride saved = rideRepository.save(ride);
        try {
            if (tryAssign(saved, caller.token())) {
                saved = rideRepository.save(saved);
            }
        } catch (ApiException e) {
            // Driver service down: the ride stays REQUESTED and can be assigned later via POST /{id}/assign
            log.warn("Ride {} created but driver assignment failed: {}", saved.getId(), e.getMessage());
        }
        return RideResponse.from(saved);
    }

    /** Retry the driver assignment of a REQUESTED ride. */
    public RideResponse assign(String id, CurrentUser caller) {
        Ride ride = findOrThrow(id);
        if (!caller.isAdmin() && !caller.userId().equals(ride.getPassengerId())) {
            throw ApiException.forbidden("You can only assign a driver to your own ride");
        }
        requireStatus(ride, RideStatus.REQUESTED);
        if (!tryAssign(ride, caller.token())) {
            throw ApiException.notFound("NO_DRIVER_AVAILABLE", "No available driver found right now - try again shortly");
        }
        return RideResponse.from(rideRepository.save(ride));
    }

    /**
     * Picks the nearest AVAILABLE driver (from the Driver Service) who has no other active ride and has not
     * rejected this ride, optionally matching the requested vehicle type. Mutates the ride (does not save).
     */
    boolean tryAssign(Ride ride, String token) {
        List<DriverInfo> available = driverClient.getAvailableDrivers(token, ride.getServiceArea());
        Set<String> occupied = rideRepository.findByStatusIn(RideStatus.DRIVER_OCCUPIED).stream()
                .map(Ride::getDriverProfileId)
                .collect(Collectors.toSet());
        String wantedType = ride.getRequestedVehicleType();

        Place pickup = ride.getPickup();
        Optional<DriverInfo> best = available.stream()
                .filter(d -> d.id() != null && d.userId() != null)
                .filter(d -> !occupied.contains(d.id()))
                .filter(d -> !ride.getRejectedDriverProfileIds().contains(d.id()))
                .filter(d -> wantedType == null
                        || (d.vehicle() != null && wantedType.equalsIgnoreCase(d.vehicle().vehicleType())))
                .min(Comparator.comparingDouble(d -> distanceToPickup(d, pickup)));
        if (best.isEmpty()) {
            return false;
        }

        DriverInfo d = best.get();
        ride.setDriverProfileId(d.id());
        ride.setDriverUserId(d.userId());
        ride.setDriverName(d.fullName());
        ride.setDriverPhone(d.phoneNumber());
        if (d.vehicle() != null) {
            ride.setVehiclePlate(d.vehicle().plateNumber());
            ride.setVehicleType(d.vehicle().vehicleType());
            ride.setVehicleModel(d.vehicle().model());
        }
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setAssignedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        return true;
    }

    private static double distanceToPickup(DriverInfo d, Place pickup) {
        if (d.currentLocation() == null || d.currentLocation().latitude() == null
                || d.currentLocation().longitude() == null) {
            return Double.MAX_VALUE;
        }
        return GeoUtil.haversineKm(d.currentLocation().latitude(), d.currentLocation().longitude(),
                pickup.getLatitude(), pickup.getLongitude());
    }

    // ------------------------------------------------------------------ driver actions

    public RideResponse accept(String id, CurrentUser caller) {
        Ride ride = findOrThrow(id);
        assertAssignedDriver(ride, caller);
        requireStatus(ride, RideStatus.ASSIGNED);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        return RideResponse.from(rideRepository.save(ride));
    }

    /** The assigned driver declines: the ride goes back to REQUESTED and another driver is looked for. */
    public RideResponse reject(String id, CurrentUser caller) {
        Ride ride = findOrThrow(id);
        assertAssignedDriver(ride, caller);
        requireStatus(ride, RideStatus.ASSIGNED);

        ride.getRejectedDriverProfileIds().add(ride.getDriverProfileId());
        clearDriver(ride);
        ride.setStatus(RideStatus.REQUESTED);
        ride.setAssignedAt(null);
        ride.setUpdatedAt(Instant.now());
        try {
            tryAssign(ride, caller.token());
        } catch (ApiException e) {
            log.warn("Re-assignment of ride {} failed: {}", ride.getId(), e.getMessage());
        }
        return RideResponse.from(rideRepository.save(ride));
    }

    public RideResponse start(String id, CurrentUser caller) {
        Ride ride = findOrThrow(id);
        assertAssignedDriver(ride, caller);
        requireStatus(ride, RideStatus.ACCEPTED);
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        Ride saved = rideRepository.save(ride);
        driverClient.setAvailability(caller.token(), saved.getDriverProfileId(), "BUSY"); // best effort
        return RideResponse.from(saved);
    }

    public RideResponse complete(String id, CompleteRideRequest req, CurrentUser caller) {
        Ride ride = findOrThrow(id);
        assertAssignedDriver(ride, caller);
        requireStatus(ride, RideStatus.IN_PROGRESS);

        double distance = req != null && req.actualDistanceKm() != null ? req.actualDistanceKm()
                : ride.getEstimatedDistanceKm() != null ? ride.getEstimatedDistanceKm() : 0.0;
        double duration = req != null && req.actualDurationMinutes() != null ? req.actualDurationMinutes()
                : ride.getEstimatedDurationMinutes() != null ? ride.getEstimatedDurationMinutes() : 0.0;

        Instant now = Instant.now();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setActualDistanceKm(distance);
        ride.setActualDurationMinutes(duration);
        ride.setCompletedAt(now);
        ride.setUpdatedAt(now);
        ride.setFareStatus(FARE_PENDING);
        Ride saved = rideRepository.save(ride);

        driverClient.setAvailability(caller.token(), saved.getDriverProfileId(), "AVAILABLE"); // best effort
        try {
            applyFinalFare(saved, caller.token());
            saved = rideRepository.save(saved);
        } catch (ApiException e) {
            // ride is COMPLETED either way; the fare can be (re)requested with POST /{id}/fare
            log.warn("Ride {} completed but final fare is pending: {}", saved.getId(), e.getMessage());
        }
        return RideResponse.from(saved);
    }

    /** Re-send the completed ride to the Fare Service (idempotent there). */
    public RideResponse syncFare(String id, CurrentUser caller) {
        Ride ride = findOrThrow(id);
        if (!caller.isAdmin()) {
            assertAssignedDriver(ride, caller);
        }
        requireStatus(ride, RideStatus.COMPLETED);
        if (!FARE_CALCULATED.equals(ride.getFareStatus())) {
            applyFinalFare(ride, caller.token()); // throws ApiException if the Fare Service is unavailable
            ride.setUpdatedAt(Instant.now());
            ride = rideRepository.save(ride);
        }
        return RideResponse.from(ride);
    }

    private void applyFinalFare(Ride ride, String token) {
        String vehicleType = ride.getVehicleType() != null ? ride.getVehicleType() : ride.getRequestedVehicleType();
        FareInfo fare = fareClient.createFinalFare(token, new FareClient.FinalFareBody(
                ride.getId(), ride.getPassengerId(), ride.getDriverUserId(), vehicleType,
                ride.getActualDistanceKm() == null ? 0.0 : ride.getActualDistanceKm(),
                ride.getActualDurationMinutes() == null ? 0.0 : ride.getActualDurationMinutes()));
        ride.setFinalFare(fare.amount());
        ride.setFareId(fare.id());
        if (fare.currency() != null) {
            ride.setCurrency(fare.currency());
        }
        ride.setFareStatus(FARE_CALCULATED);
    }

    // ------------------------------------------------------------------ cancel

    public RideResponse cancel(String id, CancelRideRequest req, CurrentUser caller) {
        Ride ride = findOrThrow(id);
        boolean isPassenger = caller.userId().equals(ride.getPassengerId());
        boolean isDriver = ride.getDriverUserId() != null && caller.userId().equals(ride.getDriverUserId());
        if (!caller.isAdmin() && !isPassenger && !isDriver) {
            throw ApiException.forbidden("You can only cancel your own ride");
        }
        if (ride.getStatus() != RideStatus.REQUESTED && ride.getStatus() != RideStatus.ASSIGNED
                && ride.getStatus() != RideStatus.ACCEPTED) {
            throw ApiException.conflict("INVALID_STATE_TRANSITION",
                    "A ride that is " + ride.getStatus() + " cannot be cancelled (only REQUESTED, ASSIGNED or ACCEPTED rides can)");
        }
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelReason(req == null ? null : blankToNull(req.reason()));
        ride.setCancelledByRole(caller.role());
        ride.setCancelledAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        return RideResponse.from(rideRepository.save(ride));
    }

    // ------------------------------------------------------------------ queries

    public RideResponse get(String id, CurrentUser caller) {
        Ride ride = findOrThrow(id);
        boolean allowed = caller.isAdmin()
                || caller.userId().equals(ride.getPassengerId())
                || caller.userId().equals(ride.getDriverUserId());
        if (!allowed) {
            throw ApiException.forbidden("You can only view your own rides");
        }
        return RideResponse.from(ride);
    }

    /** Passenger: rides they requested. Driver: rides assigned to them. Admin: every ride. */
    public List<RideResponse> mine(CurrentUser caller, RideStatus status) {
        List<Ride> rides;
        if (caller.isAdmin()) {
            rides = rideRepository.findAll();
        } else if ("DRIVER".equals(caller.role())) {
            rides = rideRepository.findByDriverUserIdOrderByRequestedAtDesc(caller.userId());
        } else {
            rides = rideRepository.findByPassengerIdOrderByRequestedAtDesc(caller.userId());
        }
        return filter(rides, status);
    }

    public List<RideResponse> listAll(RideStatus status) {
        return filter(rideRepository.findAll(), status);
    }

    // ------------------------------------------------------------------ helpers

    private List<RideResponse> filter(List<Ride> rides, RideStatus status) {
        return rides.stream()
                .filter(r -> status == null || r.getStatus() == status)
                .map(RideResponse::from)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Ride findOrThrow(String id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("RIDE_NOT_FOUND", "Ride not found with id: " + id));
    }

    private void assertAssignedDriver(Ride ride, CurrentUser caller) {
        if (ride.getDriverUserId() == null || !ride.getDriverUserId().equals(caller.userId())) {
            throw ApiException.forbidden("This ride is not assigned to you");
        }
    }

    private void requireStatus(Ride ride, RideStatus expected) {
        if (ride.getStatus() != expected) {
            throw ApiException.conflict("INVALID_STATE_TRANSITION",
                    "Ride is " + ride.getStatus() + " but this action requires it to be " + expected);
        }
    }

    private void clearDriver(Ride ride) {
        ride.setDriverProfileId(null);
        ride.setDriverUserId(null);
        ride.setDriverName(null);
        ride.setDriverPhone(null);
        ride.setVehiclePlate(null);
        ride.setVehicleType(null);
        ride.setVehicleModel(null);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

}
