package com.logibridge.backend.order.mapper;

import com.logibridge.backend.order.dto.OrderResponse;
import com.logibridge.backend.order.entity.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);
}