package com.logibridge.backend.order.controller;

import com.logibridge.backend.order.dto.CreateOrderRequest;
import com.logibridge.backend.order.dto.OrderResponse;
import com.logibridge.backend.order.dto.OrderTrackingResponse;
import com.logibridge.backend.order.dto.UpdateOrderStatusRequest;
import com.logibridge.backend.order.service.OrderQueryService;
import com.logibridge.backend.order.service.OrderService;
import com.logibridge.backend.security.service.CustomUserDetails;
import com.logibridge.backend.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @PostMapping("/orders")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        Long companyId = getCurrentUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, companyId));
    }

    @GetMapping("/orders/my")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Long companyId = getCurrentUser().getId();
        return ResponseEntity.ok(orderQueryService.getCompanyOrders(companyId, pageable));
    }

    @GetMapping("/orders/{orderNumber}")
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

    @GetMapping("/orders/{orderNumber}/tracking")
    @PreAuthorize("hasAnyRole('COMPANY','DELIVERY','ADMIN')")
    public ResponseEntity<Page<OrderTrackingResponse>> getTracking(
            @PathVariable String orderNumber,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable
    ) {
        CustomUserDetails user = getCurrentUser();

        return ResponseEntity.ok(
                orderQueryService.getOrderTracking(
                        orderNumber,
                        user.getId(),
                        user.getAuthorities(),
                        pageable
                )
        );
    }

    @PutMapping("/orders/{orderNumber}/status")
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

    @GetMapping("/delivery/orders/assigned")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<Page<OrderResponse>> getAssignedOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Long deliveryId = getCurrentUser().getId();
        return ResponseEntity.ok(
                orderQueryService.getAssignedOrders(deliveryId, pageable)
        );
    }

    @PostMapping("/delivery/orders/{orderNumber}/accept")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<OrderResponse> acceptOrder(
            @PathVariable String orderNumber
    ) {
        Long deliveryId = getCurrentUser().getId();
        return ResponseEntity.ok(
                orderService.acceptOrder(orderNumber, deliveryId)
        );
    }

    @PostMapping("/delivery/orders/{orderNumber}/reject")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable String orderNumber
    ) {
        Long deliveryId = getCurrentUser().getId();
        return ResponseEntity.ok(
                orderService.rejectOrder(orderNumber, deliveryId)
        );
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderNumber
    ) {
        Long companyId = getCurrentUser().getId();
        return ResponseEntity.ok(
                orderService.cancelOrder(orderNumber, companyId)
        );
    }

    private CustomUserDetails getCurrentUser() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new UnauthorizedException("User is not authenticated");
        }

        return user;
    }
}