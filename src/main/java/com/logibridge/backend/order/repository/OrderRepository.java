package com.logibridge.backend.order.repository;

import com.logibridge.backend.order.entity.Order;
import com.logibridge.backend.order.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByCompanyId(Long companyId, Pageable pageable);

    Page<Order> findByDeliveryCompanyId(Long deliveryCompanyId, Pageable pageable);

    Optional<Order> findByOrderNumberAndCompanyId(String orderNumber, Long companyId);

    @Query("SELECT o FROM Order o WHERE o.companyId = :companyId AND o.status = :status")
    Page<Order> findByCompanyIdAndStatus(
            @Param("companyId") Long companyId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.deliveryCompanyId = :deliveryCompanyId AND o.status = :status")
    Page<Order> findByDeliveryCompanyIdAndStatus(
            @Param("deliveryCompanyId") Long deliveryCompanyId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    boolean existsByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);
}