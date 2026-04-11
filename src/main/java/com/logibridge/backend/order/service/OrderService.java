package com.logibridge.backend.order.service;

import com.logibridge.backend.order.dto.CreateOrderRequest;
import com.logibridge.backend.order.dto.OrderResponse;
import com.logibridge.backend.order.dto.UpdateOrderStatusRequest;
import com.logibridge.backend.order.entity.Order;
import com.logibridge.backend.order.enums.OrderStatus;
import com.logibridge.backend.order.exception.InvalidOrderStateException;
import com.logibridge.backend.order.exception.OrderNotFoundException;
import com.logibridge.backend.order.mapper.OrderMapper;
import com.logibridge.backend.order.repository.OrderRepository;
import com.logibridge.backend.order.util.OrderNumberGenerator;
import com.logibridge.backend.order.validator.OrderValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderAssignmentService orderAssignmentService;
    private final OrderValidator orderValidator;


    private void logAudit(String action, Long userId, String role, String orderNumber) {
        log.info("[AUDIT] action={} userId={} role={} order={} timestamp={}",
                action,
                userId != null ? userId : "N/A",
                role != null ? role : "N/A",
                orderNumber != null ? orderNumber : "-",
                Instant.now());
    }



    @Transactional
    public OrderResponse createOrder(@Valid CreateOrderRequest request, Long companyId) {
        log.info("Creating order for companyId={}", companyId);

        Order persisted = saveWithRetry(request, companyId);

        Long deliveryCompanyId = orderAssignmentService.assignDelivery(request);


        persisted.assign(deliveryCompanyId, companyId, null);

        logAudit("ORDER_CREATED", companyId, "ROLE_COMPANY", persisted.getOrderNumber());
        logAudit("ORDER_ASSIGNED", companyId, "ROLE_COMPANY", persisted.getOrderNumber());

        return orderMapper.toResponse(persisted);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderNumber, Long companyId) {
        log.info("Company {} cancelling order={}", companyId, orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        orderValidator.validateOwnership(order, companyId);

        try {
            orderValidator.validateTransition(order, OrderStatus.CANCELLED);
            order.cancel(companyId, null);
        } catch (IllegalStateException ex) {
            throw new InvalidOrderStateException(ex.getMessage());
        }

        logAudit("ORDER_CANCELLED", companyId, "ROLE_COMPANY", orderNumber);

        return orderMapper.toResponse(order);
    }



    @Transactional
    public OrderResponse acceptOrder(String orderNumber, Long deliveryCompanyId) {
        log.info("Delivery {} accepting order={}", deliveryCompanyId, orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        orderValidator.validateDeliveryAccess(order, deliveryCompanyId);
        orderValidator.validateTransition(order, OrderStatus.ACCEPTED);

        try {
            order.accept(deliveryCompanyId, null);
        } catch (IllegalStateException ex) {
            throw new InvalidOrderStateException(ex.getMessage());
        }

        logAudit("ORDER_ACCEPTED", deliveryCompanyId, "ROLE_DELIVERY", orderNumber);

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse rejectOrder(String orderNumber, Long deliveryCompanyId) {
        log.info("Delivery {} rejecting order={}", deliveryCompanyId, orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        orderValidator.validateDeliveryAccess(order, deliveryCompanyId);
        orderValidator.validateTransition(order, OrderStatus.REJECTED);

        try {
            order.reject(deliveryCompanyId, null);
        } catch (IllegalStateException ex) {
            throw new InvalidOrderStateException(ex.getMessage());
        }

        logAudit("ORDER_REJECTED", deliveryCompanyId, "ROLE_DELIVERY", orderNumber);

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(
            String orderNumber,
            @Valid UpdateOrderStatusRequest request,
            Long deliveryCompanyId
    ) {
        log.info("Updating order status: orderNumber={}, status={}, deliveryId={}",
                orderNumber, request.getStatus(), deliveryCompanyId);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        orderValidator.validateDeliveryAccess(order, deliveryCompanyId);

        try {
            orderValidator.validateTransition(order, request.getStatus());
            applyStatusTransition(order, request.getStatus(), deliveryCompanyId, request.getLocation());

        } catch (IllegalStateException ex) {
            throw new InvalidOrderStateException(ex.getMessage());

        } catch (jakarta.persistence.OptimisticLockException ex) {
            throw new RuntimeException("Order was updated concurrently. Please retry.");
        }

        logAudit("ORDER_STATUS_UPDATED", deliveryCompanyId, "ROLE_DELIVERY", orderNumber);

        return orderMapper.toResponse(order);
    }



    @Transactional
    public OrderResponse adminForceUpdateStatus(
            String orderNumber,
            OrderStatus targetStatus,
            Long adminId
    ) {
        log.warn("ADMIN {} forcing order {} to {}", adminId, orderNumber, targetStatus);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        if (!isAdminForceAllowed(targetStatus)) {
            throw new InvalidOrderStateException(
                    "Admin can only force: IN_PROGRESS, DELIVERED, CANCELLED");
        }

        try {
            orderValidator.validateTransition(order, targetStatus);
            applyAdminTransition(order, targetStatus, adminId);

        } catch (IllegalStateException ex) {
            throw new InvalidOrderStateException(ex.getMessage());
        }

        logAudit("ADMIN_FORCE_STATUS", adminId, "ROLE_ADMIN", orderNumber);

        return orderMapper.toResponse(order);
    }


    private void applyStatusTransition(
            Order order,
            OrderStatus target,
            Long actorId,
            String location
    ) {
        switch (target) {
            case IN_PROGRESS -> order.markInProgress(actorId, location);
            case DELIVERED -> order.markDelivered(actorId, location);
            case CANCELLED -> order.cancel(actorId, location);
            default -> throw new InvalidOrderStateException(
                    "Direct transition to [" + target + "] not allowed here.");
        }
    }

    private void applyAdminTransition(Order order, OrderStatus status, Long adminId) {
        switch (status) {
            case IN_PROGRESS -> order.markInProgress(adminId, "ADMIN_OVERRIDE");
            case DELIVERED -> order.markDelivered(adminId, "ADMIN_OVERRIDE");
            case CANCELLED -> order.cancel(adminId, "ADMIN_OVERRIDE");
            default -> throw new InvalidOrderStateException("Unsupported admin status: " + status);
        }
    }

    private boolean isAdminForceAllowed(OrderStatus status) {
        return status == OrderStatus.IN_PROGRESS
                || status == OrderStatus.DELIVERED
                || status == OrderStatus.CANCELLED;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected Order saveWithRetry(CreateOrderRequest request, Long companyId) {
        int attempt = 0;
        while (true) {
            try {
                String orderNumber = orderNumberGenerator.generate();
                return orderRepository.save(Order.create(request, companyId, orderNumber));
            } catch (DataIntegrityViolationException ex) {
                if (++attempt >= 3) {
                    log.error("Could not generate unique order number after {} attempts", attempt);
                    throw ex;
                }
                log.warn("Order number collision — retrying (attempt {})", attempt);
            }
        }
    }
}