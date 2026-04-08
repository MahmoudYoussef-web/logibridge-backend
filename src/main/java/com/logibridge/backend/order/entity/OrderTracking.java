package com.logibridge.backend.order.entity;

import com.logibridge.backend.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_tracking",
        indexes = {
                @Index(name = "idx_order_tracking_order_id",  columnList = "order_id"),
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
    @Column(name = "previous_status", length = 20, updatable = false)
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20, updatable = false)
    private OrderStatus newStatus;

    @Column(updatable = false)
    private String location;

    @Column(name = "changed_by", updatable = false)
    private Long changedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // -------------------------------------------------------------------------
    // Append-only guard — prevent any mutation after persistence
    // -------------------------------------------------------------------------

    @PreUpdate
    void onPreUpdate() {
        throw new UnsupportedOperationException(
                "OrderTracking records are immutable and cannot be updated.");
    }
}