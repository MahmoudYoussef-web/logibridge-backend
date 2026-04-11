package com.logibridge.backend.order.dto;

import com.logibridge.backend.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminForceStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    private String location;
}