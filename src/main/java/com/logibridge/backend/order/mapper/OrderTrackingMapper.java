package com.logibridge.backend.order.mapper;

import com.logibridge.backend.order.dto.OrderTrackingResponse;
import com.logibridge.backend.order.entity.OrderTracking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderTrackingMapper {

    OrderTrackingResponse toResponse(OrderTracking orderTracking);
}