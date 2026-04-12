package com.logibridge.backend.order.specification;

import com.logibridge.backend.order.entity.Order;
import com.logibridge.backend.order.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public final class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<Order> hasCompanyId(Long companyId) {
        return (root, query, cb) ->
                companyId == null ? cb.conjunction()
                        : cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<Order> hasDeliveryCompanyId(Long deliveryCompanyId) {
        return (root, query, cb) ->
                deliveryCompanyId == null ? cb.conjunction()
                        : cb.equal(root.get("deliveryCompanyId"), deliveryCompanyId);
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> hasStatusIn(List<OrderStatus> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty())
                        ? cb.conjunction()
                        : root.get("status").in(statuses);
    }

    public static Specification<Order> createdAfter(LocalDateTime date) {
        return (root, query, cb) ->
                date == null ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Order> createdBefore(LocalDateTime date) {
        return (root, query, cb) ->
                date == null ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Order> withFilters(
            Long companyId,
            Long deliveryCompanyId,
            OrderStatus status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return Specification
                .where(hasCompanyId(companyId))
                .and(hasDeliveryCompanyId(deliveryCompanyId))
                .and(hasStatus(status))
                .and(createdAfter(from))
                .and(createdBefore(to));
    }
}