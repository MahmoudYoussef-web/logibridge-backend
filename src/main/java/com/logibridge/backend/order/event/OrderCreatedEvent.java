package com.logibridge.backend.order.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderCreatedEvent {

    private final String        orderNumber;
    private final Long          companyId;
    private final Long          deliveryCompanyId;
    private final LocalDateTime createdAt;
}