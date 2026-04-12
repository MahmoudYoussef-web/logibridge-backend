package com.logibridge.backend.order.service;

import com.logibridge.backend.order.entity.Order;
import com.logibridge.backend.order.entity.OrderTracking;
import com.logibridge.backend.order.enums.OrderStatus;
import com.logibridge.backend.order.repository.OrderTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    private final OrderTrackingRepository orderTrackingRepository;

    @Transactional
    public void track(
            Order order,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            Long changedBy,
            String location
    ) {
        OrderTracking tracking = OrderTracking.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .location(location)
                .changedBy(changedBy)
                .build();

        orderTrackingRepository.save(tracking);
    }
}