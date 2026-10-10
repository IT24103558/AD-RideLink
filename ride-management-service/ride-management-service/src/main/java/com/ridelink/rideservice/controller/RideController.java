package com.ridelink.rideservice.controller;

import com.ridelink.rideservice.dto.CancelRideRequest;
import com.ridelink.rideservice.dto.CompleteRideRequest;
import com.ridelink.rideservice.dto.CreateRideRequest;
import com.ridelink.rideservice.dto.RideResponse;
import com.ridelink.rideservice.model.RideStatus;
import com.ridelink.rideservice.security.CurrentUser;
import com.ridelink.rideservice.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Rides")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @Operation(summary = "Request a ride (PASSENGER). A free driver is assigned automatically; if none is free the ride stays REQUESTED.")
    @PostMapping
    public ResponseEntity<RideResponse> create(@Valid @RequestBody CreateRideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.create(request, CurrentUser.current()));
    }

    @Operation(summary = "Retry driver assignment for a REQUESTED ride (the passenger, or ADMIN). 404 NO_DRIVER_AVAILABLE if nobody is free.")
    @PostMapping("/{id}/assign")
    public ResponseEntity<RideResponse> assign(@PathVariable String id) {
        return ResponseEntity.ok(rideService.assign(id, CurrentUser.current()));
    }

    @Operation(summary = "Assigned driver accepts the ride (ASSIGNED -> ACCEPTED)")
    @PatchMapping("/{id}/accept")
    public ResponseEntity<RideResponse> accept(@PathVariable String id) {
        return ResponseEntity.ok(rideService.accept(id, CurrentUser.current()));
    }

    @Operation(summary = "Assigned driver rejects the ride (ASSIGNED -> REQUESTED, another driver is looked for)")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<RideResponse> reject(@PathVariable String id) {
        return ResponseEntity.ok(rideService.reject(id, CurrentUser.current()));
    }

    @Operation(summary = "Assigned driver starts the trip (ACCEPTED -> IN_PROGRESS); the driver is marked BUSY in the Driver Service")
    @PatchMapping("/{id}/start")
    public ResponseEntity<RideResponse> start(@PathVariable String id) {
        return ResponseEntity.ok(rideService.start(id, CurrentUser.current()));
    }

    @Operation(summary = "Assigned driver completes the trip (IN_PROGRESS -> COMPLETED). Body is optional. "
            + "The final fare is calculated by the Fare & Payment Service and the driver is marked AVAILABLE again.")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<RideResponse> complete(@PathVariable String id,
                                                 @Valid @RequestBody(required = false) CompleteRideRequest request) {
        return ResponseEntity.ok(rideService.complete(id, request, CurrentUser.current()));
    }

    @Operation(summary = "Re-send a COMPLETED ride to the Fare Service when its fareStatus is PENDING (driver of the ride, or ADMIN)")
    @PostMapping("/{id}/fare")
    public ResponseEntity<RideResponse> syncFare(@PathVariable String id) {
        return ResponseEntity.ok(rideService.syncFare(id, CurrentUser.current()));
    }

    @Operation(summary = "Cancel a ride (passenger, assigned driver or ADMIN) - only while REQUESTED, ASSIGNED or ACCEPTED. Body is optional.")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RideResponse> cancel(@PathVariable String id,
                                               @Valid @RequestBody(required = false) CancelRideRequest request) {
        return ResponseEntity.ok(rideService.cancel(id, request, CurrentUser.current()));
    }

    @Operation(summary = "My rides (passenger: rides I requested; driver: rides assigned to me; admin: all). Optional ?status= filter.")
    @GetMapping("/me")
    public ResponseEntity<List<RideResponse>> mine(@RequestParam(required = false) RideStatus status) {
        return ResponseEntity.ok(rideService.mine(CurrentUser.current(), status));
    }

    @Operation(summary = "List ALL rides (ADMIN only). Optional ?status= filter.")
    @GetMapping
    public ResponseEntity<List<RideResponse>> all(@RequestParam(required = false) RideStatus status) {
        return ResponseEntity.ok(rideService.listAll(status));
    }

    @Operation(summary = "Get a ride (its passenger, its driver, or ADMIN)")
    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(rideService.get(id, CurrentUser.current()));
    }
}
