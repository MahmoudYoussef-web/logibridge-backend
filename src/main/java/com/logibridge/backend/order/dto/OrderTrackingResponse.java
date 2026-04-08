package com.logibridge.backend.order.dto;

import com.logibridge.backend.order.enums.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTrackingResponse {

    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String location;
    private Long changedBy;
    private LocalDateTime timestamp;
}