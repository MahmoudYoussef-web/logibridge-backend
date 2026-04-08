package com.logibridge.backend.order.service;

import com.logibridge.backend.order.dto.OrderResponse;
import com.logibridge.backend.order.dto.OrderTrackingResponse;
import com.logibridge.backend.order.entity.Order;
import com.logibridge.backend.order.exception.OrderNotFoundException;
import com.logibridge.backend.order.exception.UnauthorizedOrderAccessException;
import com.logibridge.backend.order.mapper.OrderMapper;
import com.logibridge.backend.order.mapper.OrderTrackingMapper;
import com.logibridge.backend.order.repository.OrderRepository;
import com.logibridge.backend.order.repository.OrderTrackingRepository;
import com.logibridge.backend.order.specification.OrderSpecification;
import com.logibridge.backend.order.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private static final String ROLE_ADMIN    = "ROLE_ADMIN";
    private static final String ROLE_COMPANY  = "ROLE_COMPANY";
    private static final String ROLE_DELIVERY = "ROLE_DELIVERY";

    private final OrderRepository         orderRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final OrderMapper             orderMapper;
    private final OrderTrackingMapper     orderTrackingMapper;

    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber, Long userId, String role) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        enforceReadAccess(order, userId, role);

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getCompanyOrders(Long companyId, Pageable pageable) {
        return orderRepository.findByCompanyId(companyId, pageable)
                .map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getDeliveryOrders(Long deliveryCompanyId, Pageable pageable) {
        return orderRepository.findByDeliveryCompanyId(deliveryCompanyId, pageable)
                .map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<OrderTrackingResponse> getOrderTracking(
            String orderNumber,
            Long userId,
            String role
    ) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        enforceReadAccess(order, userId, role);

        return orderTrackingRepository.findByOrderIdOrderByTimestampAsc(order.getId())
                .stream()
                .map(orderTrackingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> filterOrders(
            Long companyId,
            Long deliveryCompanyId,
            OrderStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        Specification<Order> spec = OrderSpecification.withFilters(
                companyId, deliveryCompanyId, status, from, to);

        return orderRepository.findAll(spec, pageable)
                .map(orderMapper::toResponse);
    }

    private void enforceReadAccess(Order order, Long userId, String role) {
        switch (role) {
            case ROLE_ADMIN -> { /* unrestricted */ }
            case ROLE_COMPANY -> {
                if (!order.isOwnedByCompany(userId)) {
                    throw new UnauthorizedOrderAccessException(
                            "Company " + userId + " does not own order " + order.getOrderNumber());
                }
            }
            case ROLE_DELIVERY -> {
                if (!order.isAssignedToDelivery(userId)) {
                    throw new UnauthorizedOrderAccessException(
                            "Delivery company " + userId +
                                    " is not assigned to order " + order.getOrderNumber());
                }
            }
            default -> throw new UnauthorizedOrderAccessException("Unknown role: " + role);
        }
    }
}