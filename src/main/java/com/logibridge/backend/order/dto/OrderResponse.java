package com.logibridge.backend.order.dto;

import com.logibridge.backend.order.enums.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private String orderNumber;
    private Long companyId;
    private Long deliveryCompanyId;
    private String productName;
    private String description;
    private Double weight;
    private String pickupAddress;
    private String deliveryAddress;
    private OrderStatus status;
    private LocalDateTime createdAt;
}