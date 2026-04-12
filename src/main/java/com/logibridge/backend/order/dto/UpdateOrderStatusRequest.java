package com.logibridge.backend.order.dto;

import com.logibridge.backend.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;
}