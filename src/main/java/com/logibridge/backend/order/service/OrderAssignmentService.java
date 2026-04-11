package com.logibridge.backend.order.service;

import com.logibridge.backend.auth.entity.User;
import com.logibridge.backend.auth.enums.RoleName;
import com.logibridge.backend.auth.repository.UserRepository;
import com.logibridge.backend.order.dto.CreateOrderRequest;
import com.logibridge.backend.order.exception.NoDeliveryUserAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

        List<User> candidates =
                userRepository.findAllActiveUsersByRole(RoleName.ROLE_DELIVERY);

        if (candidates.isEmpty()) {
            log.error("Order assignment failed — no active delivery users found");
            throw new NoDeliveryUserAvailableException(
                    "No active delivery user available for assignment");
        }

        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        User selected = candidates.get(index);

        log.debug("Assigned delivery user id={} from {} candidates",
                selected.getId(), candidates.size());

        return selected.getId();
    }
}