package com.logibridge.backend.order.entity;

import com.logibridge.backend.order.enums.OrderStatus;
import com.logibridge.backend.order.exception.InvalidOrderStateException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "order_tracking",
        indexes = {
                @Index(name = "idx_order_tracking_order_id", columnList = "order_id"),
                @Index(name = "idx_order_tracking_timestamp", columnList = "timestamp"),
                @Index(name = "idx_order_tracking_new_status", columnList = "new_status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", updatable = false)
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, updatable = false)
    private OrderStatus newStatus;

    @Column(length = 255, updatable = false)
    private String location;

    @Column(name = "changed_by", updatable = false)
    private Long changedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    void onPrePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (newStatus == null) {
            throw new InvalidOrderStateException("newStatus must not be null");
        }
        if (previousStatus == newStatus) {
            throw new InvalidOrderStateException("previousStatus and newStatus cannot be the same");
        }
    }

    @PreUpdate
    void onPreUpdate() {
        throw new UnsupportedOperationException(
                "OrderTracking records are immutable and cannot be updated.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderTracking that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}