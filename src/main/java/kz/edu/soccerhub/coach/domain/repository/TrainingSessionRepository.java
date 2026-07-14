package kz.edu.soccerhub.coach.domain.repository;

import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

    Optional<TrainingSession> findByIdAndCoachId(UUID id, UUID coachId);

    Optional<TrainingSession> findByScheduleIdAndSessionDate(UUID scheduleId, LocalDate sessionDate);

    boolean existsByGroupIdAndSessionDateAndScheduledStartAtAndStatusNot(
            UUID groupId,
            LocalDate sessionDate,
            LocalDateTime scheduledStartAt,
            TrainingSessionStatus status
    );

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

    List<TrainingSession> findByGroupId(UUID groupId);

    List<TrainingSession> findByGroupIdAndSessionDateBetweenOrderBySessionDateAscScheduledStartAtAsc(
            UUID groupId,
            LocalDate from,
            LocalDate to
    );

    @Query("""
        select count(ts) > 0
        from TrainingSession ts
        where ts.coachId = :coachId
          and ts.sessionDate = :sessionDate
          and ts.id <> :sessionId
          and ts.status <> kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus.CANCELLED
          and ts.scheduledStartAt < :endsAt
          and ts.scheduledEndAt > :startsAt
    """)
    boolean existsCoachConflict(
            @Param("coachId") UUID coachId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startsAt") LocalDateTime startsAt,
            @Param("endsAt") LocalDateTime endsAt,
            @Param("sessionId") UUID sessionId
    );

    @Query("""
        select count(ts) > 0
        from TrainingSession ts
        where ts.locationId = :locationId
          and ts.sessionDate = :sessionDate
          and ts.id <> :sessionId
          and ts.status <> kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus.CANCELLED
          and ts.scheduledStartAt < :endsAt
          and ts.scheduledEndAt > :startsAt
    """)
    boolean existsLocationConflict(
            @Param("locationId") UUID locationId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startsAt") LocalDateTime startsAt,
            @Param("endsAt") LocalDateTime endsAt,
            @Param("sessionId") UUID sessionId
    );

    int countByCoachIdAndSessionDateBeforeAndReportDoneFalseAndStatusNot(
            UUID coachId,
            LocalDate date,
            TrainingSessionStatus status
    );

    @Modifying
    @Query("""
        update TrainingSession ts
        set ts.status = kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus.CANCELLED,
            ts.cancelReason = :reason
        where ts.scheduleId in :scheduleIds
          and ts.sessionDate >= :fromDate
          and ts.status = kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus.PLANNED
    """)
    int cancelFuturePlannedByScheduleIds(
            @Param("scheduleIds") Set<UUID> scheduleIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("reason") String reason
    );

    @Modifying
    @Query("""
        update TrainingSession ts
        set ts.status = kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus.PLANNED,
            ts.cancelReason = null
        where ts.scheduleId in :scheduleIds
          and ts.sessionDate >= :fromDate
          and ts.status = kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus.CANCELLED
          and ts.cancelReason = :reason
    """)
    int reactivateScheduleCancelledByScheduleIds(
            @Param("scheduleIds") Set<UUID> scheduleIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("reason") String reason
    );
}
