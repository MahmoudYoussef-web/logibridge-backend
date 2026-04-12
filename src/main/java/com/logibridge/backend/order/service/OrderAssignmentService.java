package com.logibridge.backend.order.service;

import com.logibridge.backend.auth.entity.User;
import com.logibridge.backend.auth.repository.UserRepository;
import com.logibridge.backend.order.dto.CreateOrderRequest;
import com.logibridge.backend.order.exception.NoDeliveryUserAvailableException;
import com.logibridge.backend.order.exception.InvalidOrderStateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderAssignmentService {

    private final UserRepository userRepository;

    public Long assignDelivery(CreateOrderRequest request) {

        if (request.getDeliveryCompanyId() != null) {
            User user = userRepository.findById(request.getDeliveryCompanyId())
                    .orElseThrow(() -> new InvalidOrderStateException("Invalid delivery user"));

            return user.getId();
        }

        return resolveAvailableDeliveryCompany();
    }

    private Long resolveAvailableDeliveryCompany() {

        List<User> candidates =
                userRepository.findAllActiveUsersByRole(
                        com.logibridge.backend.auth.enums.RoleName.ROLE_DELIVERY
                );

        if (candidates.isEmpty()) {
            throw new NoDeliveryUserAvailableException(
                    "No active delivery user available for assignment");
        }

        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(index).getId();
    }
}