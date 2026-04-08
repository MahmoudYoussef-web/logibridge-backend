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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @PostMapping
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        Long companyId = resolveUserId();
        OrderResponse response = orderService.createOrder(request, companyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Long companyId = resolveUserId();
        return ResponseEntity.ok(orderQueryService.getCompanyOrders(companyId, pageable));
    }

    @GetMapping("/delivery")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<Page<OrderResponse>> getAssignedOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Long deliveryCompanyId = resolveUserId();
        return ResponseEntity.ok(orderQueryService.getDeliveryOrders(deliveryCompanyId, pageable));
    }

    @GetMapping("/admin")
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

    @GetMapping("/{orderNumber}")
    @PreAuthorize("hasAnyRole('COMPANY', 'DELIVERY', 'ADMIN')")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(
            @PathVariable String orderNumber
    ) {
        Long userId = resolveUserId();
        String role = resolveRole();
        return ResponseEntity.ok(orderQueryService.getOrderByOrderNumber(orderNumber, userId, role));
    }

    @PutMapping("/{orderNumber}/status")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        Long deliveryCompanyId = resolveUserId();
        return ResponseEntity.ok(orderService.updateOrderStatus(orderNumber, request, deliveryCompanyId));
    }

    @GetMapping("/{orderNumber}/tracking")
    @PreAuthorize("hasAnyRole('COMPANY', 'DELIVERY', 'ADMIN')")
    public ResponseEntity<List<OrderTrackingResponse>> getOrderTracking(
            @PathVariable String orderNumber
    ) {
        Long userId = resolveUserId();
        String role = resolveRole();
        return ResponseEntity.ok(orderQueryService.getOrderTracking(orderNumber, userId, role));
    }

    private Long resolveUserId() {
        return resolvePrincipal().getId();
    }

    private String resolveRole() {
        return resolvePrincipal().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No role found"));
    }

    private CustomUserDetails resolvePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }
}