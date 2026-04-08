package com.logibridge.backend.order.entity;


import com.logibridge.backend.order.dto.CreateOrderRequest;
import com.logibridge.backend.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_order_number",     columnList = "order_number"),
                @Index(name = "idx_orders_company_id",       columnList = "company_id"),
                @Index(name = "idx_orders_delivery_company", columnList = "delivery_company_id"),
                @Index(name = "idx_orders_status",           columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "delivery_company_id")
    private Long deliveryCompanyId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Double weight;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<OrderTracking> trackingHistory = new ArrayList<>();



    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
    }



    public void initialize() {
        if (this.status != null) {
            throw new IllegalStateException("Order has already been initialized.");
        }
        this.status    = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void markInProgress(Long changedBy, String location) {
        changeStatus(OrderStatus.IN_PROGRESS, changedBy, location);
    }

    public void markDelivered(Long changedBy, String location) {
        changeStatus(OrderStatus.DELIVERED, changedBy, location);
    }

    public void cancel(Long changedBy, String location) {
        changeStatus(OrderStatus.CANCELLED, changedBy, location);
    }

    public void assignDeliveryCompany(Long deliveryCompanyId) {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Delivery company can only be assigned when order is PENDING.");
        }
        if (this.deliveryCompanyId != null) {
            throw new IllegalStateException(
                    "Order is already assigned to a delivery company (id=" + this.deliveryCompanyId + ").");
        }
        this.deliveryCompanyId = deliveryCompanyId;
    }

    public void changeStatus(OrderStatus newStatus, Long changedBy, String location) {
        if (this.status == null) {
            throw new IllegalStateException("Order must be initialized before changing status.");
        }
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition: %s → %s", this.status, newStatus));
        }

        OrderTracking tracking = OrderTracking.builder()
                .order(this)
                .previousStatus(this.status)
                .newStatus(newStatus)
                .location(location)
                .changedBy(changedBy)
                .timestamp(LocalDateTime.now())
                .build();

        this.trackingHistory.add(tracking);
        this.status = newStatus;
    }



    public boolean isOwnedByCompany(Long companyId) {
        return this.companyId != null && this.companyId.equals(companyId);
    }

    public boolean isAssignedToDelivery(Long deliveryCompanyId) {
        return this.deliveryCompanyId != null && this.deliveryCompanyId.equals(deliveryCompanyId);
    }

    public List<OrderTracking> getTrackingHistory() {
        return Collections.unmodifiableList(trackingHistory);
    }


    public static OrderBuilder builder() {
        return new ValidatingOrderBuilder();
    }

    private static class ValidatingOrderBuilder extends OrderBuilder {

        @Override
        public Order build() {
            if (super.weight != null && super.weight <= 0) {
                throw new IllegalArgumentException(
                        "Weight must be a positive value, got: " + super.weight);
            }
            return super.build();
        }
    }

    public static Order create(CreateOrderRequest request, Long companyId, String orderNumber) {
        Order order = Order.builder()
                .companyId(companyId)
                .orderNumber(orderNumber)
                .productName(request.getProductName())
                .description(request.getDescription())
                .weight(request.getWeight())
                .pickupAddress(request.getPickupAddress())
                .deliveryAddress(request.getDeliveryAddress())
                .build();
        order.initialize();
        return order;
    }
}