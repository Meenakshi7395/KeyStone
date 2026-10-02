package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderFilter;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Builds the role-scoped, filtered query behind GET /api/work-orders. */
final class WorkOrderSpecifications {

    private static final List<WorkOrderStatus> ACTIVE = List.of(
            WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD);

    private WorkOrderSpecifications() {
    }

    static Specification<WorkOrder> build(User caller, WorkOrderFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            // Role scope — enforced on the server whatever filters are sent.
            if (caller != null && caller.getRole() == Role.TECHNICIAN) {
                p.add(cb.equal(root.get("technician").get("id"), caller.getId()));
            } else if (caller != null && caller.getRole() == Role.CUSTOMER) {
                Integer own = caller.getCustomer() != null ? caller.getCustomer().getId() : -1;
                p.add(cb.equal(root.get("customer").get("id"), own));
            }

            if (f.statuses() != null && !f.statuses().isEmpty()) {
                p.add(root.get("status").in(f.statuses()));
            }
            if (f.priority() != null) {
                p.add(cb.equal(root.get("priority"), f.priority()));
            }
            if (Boolean.TRUE.equals(f.unassigned())) {
                p.add(cb.isNull(root.get("technician")));
            } else if (f.technicianId() != null) {
                p.add(cb.equal(root.get("technician").get("id"), f.technicianId()));
            }
            if (f.siteId() != null) {
                p.add(cb.equal(root.get("site").get("id"), f.siteId()));
            }
            if (f.customerId() != null) {
                p.add(cb.equal(root.get("customer").get("id"), f.customerId()));
            }
            if (Boolean.TRUE.equals(f.overdue())) {
                p.add(root.get("status").in(ACTIVE));
                p.add(cb.lessThan(root.<Instant>get("slaDueDate"), Instant.now()));
            }
            if (f.from() != null) {
                Instant start = f.from().atStartOfDay().toInstant(ZoneOffset.UTC);
                p.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), start));
            }
            if (f.to() != null) {
                Instant end = f.to().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
                p.add(cb.lessThan(root.<Instant>get("createdAt"), end));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase() + "%";
                Join<Object, Object> tech = root.join("technician", JoinType.LEFT);
                p.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("customer").get("name")), like),
                        cb.like(cb.lower(root.get("site").get("name")), like),
                        cb.like(cb.lower(tech.get("name")), like)
                ));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
