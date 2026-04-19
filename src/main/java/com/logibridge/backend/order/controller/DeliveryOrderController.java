package com.logibridge.backend.order.controller;

import com.logibridge.backend.order.dto.OrderResponse;
import com.logibridge.backend.order.service.OrderQueryService;
import com.logibridge.backend.order.service.OrderService;
import com.logibridge.backend.security.service.CustomUserDetails;
import com.logibridge.backend.common.exception.UnauthorizedException;
import com.logibridge.backend.order.exception.InvalidOrderStateException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery/orders")
@RequiredArgsConstructor
public class DeliveryOrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('DELIVERY')")
    public Page<OrderResponse> getAssignedOrders(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Long deliveryId = getUserId(authentication);
        return orderQueryService.getDeliveryOrders(deliveryId, pageable);
    }

    @PostMapping("/{orderNumber}/accept")
    @PreAuthorize("hasRole('DELIVERY')")
    public OrderResponse acceptOrder(
            @PathVariable String orderNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication
    ) {
        validateIdempotencyKey(idempotencyKey);

        Long deliveryId = getUserId(authentication);
        return orderService.acceptOrder(orderNumber, deliveryId, idempotencyKey);
    }

    @PostMapping("/{orderNumber}/reject")
    @PreAuthorize("hasRole('DELIVERY')")
    public OrderResponse rejectOrder(
            @PathVariable String orderNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication
    ) {
        validateIdempotencyKey(idempotencyKey);

        Long deliveryId = getUserId(authentication);
        return orderService.rejectOrder(orderNumber, deliveryId, idempotencyKey);
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return user.getId();
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidOrderStateException("Missing Idempotency-Key header");
        }
    }
}