package com.logibridge.backend.order.repository;


import com.logibridge.backend.order.entity.OrderTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {

    List<OrderTracking> findByOrderIdOrderByTimestampAsc(Long orderId);
    Page<OrderTracking> findByOrderIdOrderByTimestampAsc(Long orderId, Pageable pageable);
}
