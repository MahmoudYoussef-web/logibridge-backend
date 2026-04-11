package com.logibridge.backend.order.controller.admin;


import com.logibridge.backend.order.dto.AdminForceStatusRequest;
import com.logibridge.backend.order.dto.OrderResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;


    @GetMapping
    public ResponseEntity<Page<OrderResponse>> filterOrders(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long deliveryCompanyId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {

        return ResponseEntity.ok(
                orderQueryService.filterOrders(
                        companyId,
                        deliveryCompanyId,
                        status,
                        fromDate,
                        toDate,
                        pageable
                )
        );
    }



    @PutMapping("/{orderNumber}/force-status")
    public ResponseEntity<OrderResponse> forceStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody AdminForceStatusRequest request
    ) {

        Long adminId = getCurrentUser().getId();

        return ResponseEntity.ok(
                orderService.adminForceUpdateStatus(
                        orderNumber,
                        request.getStatus(),
                        adminId
                )
        );
    }



    private CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }
}