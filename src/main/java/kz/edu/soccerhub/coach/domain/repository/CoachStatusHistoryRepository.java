package kz.edu.soccerhub.coach.domain.repository;

import kz.edu.soccerhub.coach.domain.model.CoachStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoachStatusHistoryRepository extends JpaRepository<CoachStatusHistory, UUID> {
    List<CoachStatusHistory> findByCoachIdOrderByChangedAtDesc(UUID coachId);
}
