package kz.edu.soccerhub.organization.domain.repository;


import kz.edu.soccerhub.organization.domain.model.GroupCoach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupCoachRepository extends JpaRepository<GroupCoach, UUID> {
    Optional<GroupCoach> findByGroupIdAndCoachId(UUID groupId, UUID coachId);
    boolean existsByGroupIdAndCoachIdAndActiveTrue(UUID groupId, UUID coachId);
    List<GroupCoach> findByGroupIdAndActiveTrue(UUID groupId);
}
