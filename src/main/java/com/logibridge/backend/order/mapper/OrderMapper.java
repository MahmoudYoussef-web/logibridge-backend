package com.logibridge.backend.order.mapper;

import com.logibridge.backend.order.dto.OrderResponse;
import com.logibridge.backend.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "trackingCount", expression = "java(order.getTrackingHistory().size())")
    OrderResponse toResponse(Order order);
}