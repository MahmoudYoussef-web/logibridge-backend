package com.logibridge.backend.order.service;

import com.logibridge.backend.order.dto.OrderResponse;
import com.logibridge.backend.order.dto.OrderTrackingResponse;
import com.logibridge.backend.order.entity.Order;
import com.logibridge.backend.order.enums.OrderStatus;
import com.logibridge.backend.order.exception.OrderNotFoundException;
import com.logibridge.backend.order.exception.UnauthorizedOrderAccessException;
import com.logibridge.backend.order.mapper.OrderMapper;
import com.logibridge.backend.order.mapper.OrderTrackingMapper;
import com.logibridge.backend.order.repository.OrderRepository;
import com.logibridge.backend.order.repository.OrderTrackingRepository;
import com.logibridge.backend.order.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final OrderMapper orderMapper;
    private final OrderTrackingMapper orderTrackingMapper;

    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(
            String orderNumber,
            Long userId,
            Collection<? extends GrantedAuthority> authorities
    ) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        enforceReadAccess(order, userId, authorities);

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getCompanyOrders(Long companyId, Pageable pageable) {
        return orderRepository.findByCompanyId(companyId, pageable)
                .map(orderMapper::toResponse);
    }


    @Transactional(readOnly = true)
    public Page<OrderResponse> getDeliveryOrders(Long deliveryCompanyId, Pageable pageable) {

        Specification<Order> spec = Specification
                .where(OrderSpecification.hasDeliveryCompanyId(deliveryCompanyId))
                .and(OrderSpecification.hasStatusIn(getActiveDeliveryStatuses()));

        return orderRepository.findAll(spec, pageable)
                .map(orderMapper::toResponse);
    }


    private List<OrderStatus> getActiveDeliveryStatuses() {
        return List.of(
                OrderStatus.ASSIGNED,
                OrderStatus.ACCEPTED,
                OrderStatus.IN_PROGRESS
        );
    }

    @Transactional(readOnly = true)
    public Page<OrderTrackingResponse> getOrderTracking(
            String orderNumber,
            Long userId,
            Collection<? extends GrantedAuthority> authorities,
            Pageable pageable
    ) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        enforceReadAccess(order, userId, authorities);

        return orderTrackingRepository
                .findByOrderIdOrderByTimestampAsc(order.getId(), pageable)
                .map(orderTrackingMapper::toResponse);
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

    private void enforceReadAccess(
            Order order,
            Long userId,
            Collection<? extends GrantedAuthority> authorities
    ) {

        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) return;

        boolean isCompany = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY"));

        if (isCompany && order.isOwnedByCompany(userId)) return;

        boolean isDelivery = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DELIVERY"));

        if (isDelivery && order.isAssignedToDelivery(userId)) return;

        throw new UnauthorizedOrderAccessException("Access denied for this order");
    }
}