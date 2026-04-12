package com.logibridge.backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String productName;

    @Size(max = 1000)
    private String description;

    @Positive(message = "Weight must be a positive value")
    private Double weight;

    @NotBlank(message = "Pickup address is required")
    @Size(max = 500)
    private String pickupAddress;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500)
    private String deliveryAddress;

    @Positive(message = "Delivery company id must be positive")
    private Long deliveryCompanyId;
}