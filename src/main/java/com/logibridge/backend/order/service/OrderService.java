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
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderAssignmentService orderAssignmentService;
    private final OrderTrackingService orderTrackingService;
    private final OrderValidator orderValidator;

    @Transactional
    public OrderResponse createOrder(@Valid CreateOrderRequest request, Long companyId) {

        log.info("Creating order for companyId={}", companyId);

        String orderNumber = orderNumberGenerator.generate();

        Order order = Order.create(request, companyId, orderNumber);

        Long deliveryCompanyId = orderAssignmentService.assignDelivery(request);

        order.assignDeliveryCompany(deliveryCompanyId);

        // Validate ownership consistency before persisting
        orderValidator.validateOwnership(order, companyId);

        Order saved = orderRepository.save(order);

        orderTrackingService.track(
                saved,
                null,
                saved.getStatus(),
                companyId,
                null
        );

        log.info("Order created: orderNumber={}, companyId={}, deliveryCompanyId={}",
                saved.getOrderNumber(), companyId, deliveryCompanyId);

        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse updateOrderStatus(
            String orderNumber,
            @Valid UpdateOrderStatusRequest request,
            Long deliveryCompanyId
    ) {

        log.info("Updating order status: orderNumber={}, requestedStatus={}, deliveryCompanyId={}",
                orderNumber, request.getStatus(), deliveryCompanyId);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        // Delegate access check to validator (replaces inline if-block)
        orderValidator.validateDeliveryAccess(order, deliveryCompanyId);

        OrderStatus previousStatus = order.getStatus();

        applyStatusTransition(order, request.getStatus(), deliveryCompanyId, request.getLocation());

        Order saved = orderRepository.save(order);

        orderTrackingService.track(
                saved,
                previousStatus,
                saved.getStatus(),
                deliveryCompanyId,
                request.getLocation()
        );

        log.info("Order status updated: orderNumber={}, previousStatus={}, newStatus={}",
                orderNumber, previousStatus, saved.getStatus());

        return orderMapper.toResponse(saved);
    }

    private void applyStatusTransition(
            Order order,
            OrderStatus target,
            Long actorId,
            String location
    ) {
        try {
            switch (target) {
                case IN_PROGRESS -> order.markInProgress(actorId, location);
                case DELIVERED    -> order.markDelivered(actorId, location);
                case CANCELLED    -> order.cancel(actorId, location);
                default -> throw new InvalidOrderStateException(
                        "Transition to status " + target + " is not supported");
            }
        } catch (IllegalStateException ex) {
            throw new InvalidOrderStateException(ex.getMessage());
        }
    }
}