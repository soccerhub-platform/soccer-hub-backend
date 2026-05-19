package kz.edu.soccerhub.coach.domain.repository;

import kz.edu.soccerhub.coach.domain.model.TrainingSessionAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingSessionAttendanceRepository extends JpaRepository<TrainingSessionAttendance, UUID> {

    List<TrainingSessionAttendance> findBySessionId(UUID sessionId);
}
