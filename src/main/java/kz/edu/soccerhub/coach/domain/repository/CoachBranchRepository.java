package kz.edu.soccerhub.coach.domain.repository;

import kz.edu.soccerhub.coach.domain.model.CoachBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CoachBranchRepository extends JpaRepository<CoachBranch, UUID> {
    List<CoachBranch> findAllByCoachIdIn(Set<UUID> coachId);
    boolean existsByCoachIdAndBranchId(UUID coachId, UUID branchId);
    List<CoachBranch> findAllByBranchIdIn(Set<UUID> branchId);
}