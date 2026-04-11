package com.logibridge.backend.order.controller;

import com.logibridge.backend.order.dto.CreateOrderRequest;
import com.logibridge.backend.order.dto.OrderResponse;
import com.logibridge.backend.order.dto.OrderTrackingResponse;
import com.logibridge.backend.order.dto.UpdateOrderStatusRequest;
import com.logibridge.backend.order.enums.OrderStatus;
import com.logibridge.backend.order.service.OrderQueryService;
import com.logibridge.backend.order.service.OrderService;
import com.logibridge.backend.security.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;



    @PostMapping("/api/orders")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        Long companyId = getCurrentUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, companyId));
    }

    @GetMapping("/api/orders/my")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Long companyId = getCurrentUser().getId();
        return ResponseEntity.ok(orderQueryService.getCompanyOrders(companyId, pageable));
    }



    @GetMapping("/api/orders/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> filterOrders(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long deliveryCompanyId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
                orderQueryService.filterOrders(
                        companyId, deliveryCompanyId, status, fromDate, toDate, pageable));
    }



    @GetMapping("/api/orders/{orderNumber}")
    @PreAuthorize("hasAnyRole('COMPANY','DELIVERY','ADMIN')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderNumber) {

        CustomUserDetails user = getCurrentUser();

        return ResponseEntity.ok(
                orderQueryService.getOrderByOrderNumber(
                        orderNumber,
                        user.getId(),
                        user.getAuthorities()
                )
        );
    }

    @PutMapping("/api/orders/{orderNumber}/status")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        Long deliveryId = getCurrentUser().getId();
        return ResponseEntity.ok(
                orderService.updateOrderStatus(orderNumber, request, deliveryId)
        );
    }

    @GetMapping("/api/orders/{orderNumber}/tracking")
    @PreAuthorize("hasAnyRole('COMPANY','DELIVERY','ADMIN')")
    public ResponseEntity<List<OrderTrackingResponse>> getTracking(
            @PathVariable String orderNumber
    ) {
        CustomUserDetails user = getCurrentUser();

        return ResponseEntity.ok(
                orderQueryService.getOrderTracking(
                        orderNumber,
                        user.getId(),
                        user.getAuthorities() // 🔥 FIX
                )
        );
    }



    @GetMapping("/api/delivery/orders/assigned")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<Page<OrderResponse>> getAssignedOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Long deliveryId = getCurrentUser().getId();
        return ResponseEntity.ok(
                orderQueryService.getDeliveryOrders(deliveryId, pageable)
        );
    }

    @PostMapping("/api/delivery/orders/{orderNumber}/accept")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<OrderResponse> acceptOrder(
            @PathVariable String orderNumber
    ) {
        Long deliveryId = getCurrentUser().getId();
        return ResponseEntity.ok(
                orderService.acceptOrder(orderNumber, deliveryId)
        );
    }

    @PostMapping("/api/delivery/orders/{orderNumber}/reject")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable String orderNumber
    ) {
        Long deliveryId = getCurrentUser().getId();
        return ResponseEntity.ok(
                orderService.rejectOrder(orderNumber, deliveryId)
        );
    }



    private CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }
}