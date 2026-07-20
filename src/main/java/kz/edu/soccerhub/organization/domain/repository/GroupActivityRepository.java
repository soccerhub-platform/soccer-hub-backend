package kz.edu.soccerhub.organization.domain.repository;

import kz.edu.soccerhub.organization.domain.model.GroupActivity;
import kz.edu.soccerhub.organization.domain.model.enums.GroupActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GroupActivityRepository extends JpaRepository<GroupActivity, UUID> {

    Page<GroupActivity> findByGroupIdOrderByOccurredAtDesc(UUID groupId, Pageable pageable);

    List<GroupActivity> findByActivityTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            GroupActivityType activityType,
            LocalDateTime from,
            LocalDateTime to
    );
}
