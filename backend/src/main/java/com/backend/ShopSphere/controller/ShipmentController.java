package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Slf4j
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {

        log.debug(
                "Request received to create shipment for sellerOrderItemId={}",
                request.getSellerOrderItemId()
        );

        ShipmentResponse response = shipmentService.createShipment(
                request.getSellerOrderItemId(),
                request.getCarrier()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{shipmentId}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody UpdateShipmentStatusRequest request) {

        log.debug(
                "Request received to update shipment status. shipmentId={}, status={}",
                shipmentId,
                request.getStatus()
        );

        ShipmentResponse response = shipmentService.updateForwardShipmentStatus(
                shipmentId,
                request.getStatus()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/return/{shipmentId}/status")
    public ResponseEntity<ReturnShipmentResponse> updateReturnShipmentStatus(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody UpdateShipmentStatusRequest request) {

        log.debug(
                "Request received to update return shipment status. shipmentId={}, status={}",
                shipmentId,
                request.getStatus()
        );

        ReturnShipmentResponse response = shipmentService.updateReturnShipmentStatus(shipmentId, request.getStatus());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{shipmentId}")
    public ResponseEntity<ShipmentResponse> getShipment(
            @PathVariable UUID shipmentId) {

        return ResponseEntity.ok(
                shipmentService.getShipment(shipmentId)
        );
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ShipmentResponse>> getOrderShipments(
            @PathVariable UUID orderId) {

        return ResponseEntity.ok(
                shipmentService.getOrderShipments(orderId)
        );
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> trackShipment(
            @PathVariable String trackingNumber) {

        return ResponseEntity.ok(
                shipmentService.trackShipment(trackingNumber)
        );
    }

    @PostMapping("/{shipmentId}/cancel-after-delivery-failure")
    public ResponseEntity<Void> cancelAfterDeliveryFailure(
            @PathVariable UUID shipmentId) {

        shipmentService.cancelAfterDeliveryFailure(shipmentId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/return/shipment/create")
    public ResponseEntity<ReturnShipmentResponse> createReturnShipment(@Valid @RequestBody CreateReturnShipmentRequest returnShipmentRequest){
        return ResponseEntity.ok(shipmentService.createReturnShipment(returnShipmentRequest.getReturnId(), returnShipmentRequest.getCarrier()));
    }
}