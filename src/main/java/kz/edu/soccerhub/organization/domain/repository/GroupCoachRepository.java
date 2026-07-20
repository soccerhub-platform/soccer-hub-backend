package kz.edu.soccerhub.organization.domain.repository;


import kz.edu.soccerhub.organization.domain.model.GroupCoach;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface GroupCoachRepository extends JpaRepository<GroupCoach, UUID> {
    boolean existsByGroupIdAndCoachIdAndActiveTrue(UUID groupId, UUID coachId);
    List<GroupCoach> findByGroupIdAndActiveTrue(UUID groupId);
    List<GroupCoach> findByGroupIdOrderByAssignedFromDescCreatedAtDesc(UUID groupId);
    List<GroupCoach> findByCoachIdOrderByAssignedFromDescCreatedAtDesc(UUID coachId);
    List<GroupCoach> findByCoachIdAndActiveTrue(UUID coachId);
    List<GroupCoach> findByCoachIdInAndGroupIdInAndActiveTrue(Set<UUID> coachIds, Set<UUID> groupIds);
    int countByGroupIdAndActiveTrue(UUID groupId);

    boolean existsByGroupIdAndRoleAndActiveTrue(UUID groupId, CoachRole role);
}
