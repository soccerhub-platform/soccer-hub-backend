package kz.edu.soccerhub.crm.infrastructure;

import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class LeadSpecification {

    private LeadSpecification() {
    }

    public static Specification<Lead> hasStatuses(List<LeadStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Lead> hasAssignedAdmin(UUID adminId) {
        return (root, query, cb) -> cb.equal(root.get("assignedAdminId"), adminId);
    }

    public static Specification<Lead> hasBranch(UUID branchId) {
        return (root, query, cb) -> cb.equal(root.get("branchId"), branchId);
    }

    public static Specification<Lead> isUnassigned() {
        return (root, query, cb) -> cb.isNull(root.get("assignedAdminId"));
    }

    public static Specification<Lead> search(String queryText) {
        return (root, query, cb) -> {
            String queryValue = "%" + queryText.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("parentName")), queryValue),
                    cb.like(cb.lower(root.get("phone")), queryValue)
            );
        };
    }

    public static Specification<Lead> createdFrom(LocalDate date) {
        LocalDateTime fromDateTime = date.atStartOfDay();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime);
    }

    public static Specification<Lead> createdTo(LocalDate date) {
        LocalDateTime toDateTime = date.atTime(LocalTime.MAX);
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toDateTime);
    }

    public static Specification<Lead> build(
            List<LeadStatus> statuses,
            UUID assignedAdminId,
            UUID branchId,
            Boolean unassigned,
            String search,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
        Specification<Lead> specification = (root, query, cb) -> cb.conjunction();

        if (statuses != null && !statuses.isEmpty()) {
            specification = specification.and(hasStatuses(statuses));
        }

        if (assignedAdminId != null) {
            specification = specification.and(hasAssignedAdmin(assignedAdminId));
        }

        if (branchId != null) {
            specification = specification.and(hasBranch(branchId));
        }

        if (Boolean.TRUE.equals(unassigned)) {
            specification = specification.and(isUnassigned());
        }

        if (search != null && !search.trim().isEmpty()) {
            specification = specification.and(search(search.trim()));
        }

        if (createdFrom != null) {
            specification = specification.and(createdFrom(createdFrom));
        }

        if (createdTo != null) {
            specification = specification.and(createdTo(createdTo));
        }

        return specification;
    }
}

