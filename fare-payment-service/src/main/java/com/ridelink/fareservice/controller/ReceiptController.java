package com.ridelink.fareservice.controller;

import com.ridelink.fareservice.dto.ErrorResponse;
import com.ridelink.fareservice.dto.ReceiptResponse;
import com.ridelink.fareservice.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/receipts", "/api/v1/receipts"})
@RequiredArgsConstructor
@Tag(name = "Receipt Management", description = "Endpoints for retrieving issued receipts by ride ID")
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get receipt by ride ID",
            description = "Retrieves or generates an official receipt for the successful payment associated with a ride.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt found or issued",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payment for this ride failed; no receipt can be issued",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No successful payment found for this ride ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReceiptResponse> getReceiptByRideId(@PathVariable String rideId) {
        ReceiptResponse response = receiptService.getReceiptByRideId(rideId);
        return ResponseEntity.ok(response);
    }
}
