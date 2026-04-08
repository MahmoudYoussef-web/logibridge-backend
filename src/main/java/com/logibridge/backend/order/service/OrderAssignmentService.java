package com.logibridge.backend.order.service;

import com.logibridge.backend.auth.enums.RoleName;
import com.logibridge.backend.auth.repository.UserRepository;
import com.logibridge.backend.order.dto.CreateOrderRequest;
import com.logibridge.backend.order.exception.NoDeliveryUserAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAssignmentService {

    private final UserRepository userRepository;

    public Long assignDelivery(CreateOrderRequest request) {
        if (request.getDeliveryCompanyId() != null) {
            log.debug("Using explicitly requested delivery company id={}",
                    request.getDeliveryCompanyId());
            return request.getDeliveryCompanyId();
        }
        return resolveAvailableDeliveryCompany();
    }

    private Long resolveAvailableDeliveryCompany() {
        return userRepository.findFirstActiveUserByRole(RoleName.ROLE_DELIVERY)
                .map(user -> {
                    log.debug("Dynamically assigned delivery user id={}", user.getId());
                    return user.getId();
                })
                .orElseThrow(() -> {
                    log.error("Order assignment failed — no active delivery user found in the system");
                    return new NoDeliveryUserAvailableException(
                            "No active delivery user available for assignment");
                });
    }
}