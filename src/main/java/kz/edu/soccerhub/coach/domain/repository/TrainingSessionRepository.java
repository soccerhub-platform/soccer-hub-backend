package kz.edu.soccerhub.coach.domain.repository;

import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

    Optional<TrainingSession> findByIdAndCoachId(UUID id, UUID coachId);

    Optional<TrainingSession> findByScheduleIdAndSessionDate(UUID scheduleId, LocalDate sessionDate);

    List<TrainingSession> findByCoachIdAndSessionDateOrderByScheduledStartAtAsc(UUID coachId, LocalDate sessionDate);

    List<TrainingSession> findByCoachIdAndSessionDateBetweenOrderBySessionDateAscScheduledStartAtAsc(
            UUID coachId,
            LocalDate dateFrom,
            LocalDate dateTo
    );

    Page<TrainingSession> findByCoachIdAndSessionDateBetween(
            UUID coachId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    );

    List<TrainingSession> findByCoachIdInAndGroupIdInAndSessionDateBetween(
            Set<UUID> coachIds,
            Set<UUID> groupIds,
            LocalDate dateFrom,
            LocalDate dateTo
    );

    List<TrainingSession> findByCoachIdInAndGroupIdInAndSessionDateBeforeAndReportDoneFalse(
            Set<UUID> coachIds,
            Set<UUID> groupIds,
            LocalDate date
    );

    List<TrainingSession> findByCoachIdInAndGroupIdInAndReportDoneTrue(
            Set<UUID> coachIds,
            Set<UUID> groupIds
    );

    List<TrainingSession> findByCoachIdAndSessionDateGreaterThanEqualOrderBySessionDateAscScheduledStartAtAsc(
            UUID coachId,
            LocalDate date
    );

    List<TrainingSession> findByCoachIdAndReportDoneTrueOrderByUpdatedAtDesc(UUID coachId);

    int countByCoachIdAndSessionDateBeforeAndReportDoneFalseAndStatusNot(
            UUID coachId,
            LocalDate date,
            TrainingSessionStatus status
    );
}
