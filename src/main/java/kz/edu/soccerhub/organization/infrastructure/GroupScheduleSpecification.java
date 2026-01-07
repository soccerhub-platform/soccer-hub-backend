package kz.edu.soccerhub.organization.infrastructure;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import kz.edu.soccerhub.organization.application.dto.ScheduleSearchCriteria;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;
import kz.edu.soccerhub.organization.domain.model.Location;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class GroupScheduleSpecification {

    public static Specification<GroupSchedule> byCriteria(ScheduleSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.groupId() != null)
                predicates.add(cb.equal(root.get("groupId"), criteria.groupId()));

            if (criteria.coachId() != null)
                predicates.add(cb.equal(root.get("coachId"), criteria.coachId()));

            if (criteria.branchId() != null) {
                Join<GroupSchedule, Location> locationJoin = root.join("locationId", JoinType.INNER);
                predicates.add(cb.equal(locationJoin.get("branchId"), criteria.branchId()));
            }

            if (criteria.dayOfWeek() != null)
                predicates.add(cb.equal(root.get("dayOfWeek"), criteria.dayOfWeek()));

            if (criteria.status() != null)
                predicates.add(cb.equal(root.get("status"), criteria.status()));

            if (criteria.fromDate() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), criteria.fromDate()).not());

            if (criteria.toDate() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), criteria.toDate()).not());

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
