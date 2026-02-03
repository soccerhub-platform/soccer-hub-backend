package kz.edu.soccerhub.organization.domain.repository;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface GroupScheduleRepository extends
        JpaRepository<GroupSchedule, UUID>,
        JpaSpecificationExecutor<GroupSchedule> {

    List<GroupSchedule> findByGroupIdAndDayOfWeekAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID groupId,
            DayOfWeek dayOfWeek,
            ScheduleStatus status,
            LocalDate endDate,
            LocalDate startDate
    );

    @Query(value = """
        select gs
        from GroupSchedule gs
        where gs.coachId = :coachId
            and gs.dayOfWeek = :dayOfWeek
            and gs.status = 'ACTIVE'
            and gs.startDate <= :endDate
            and gs.endDate >= :startDate
    """)
    List<GroupSchedule> findCoachConflicts(
            @Param("coachId") UUID coachId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<GroupSchedule> findByGroupIdAndStatus(@NotNull UUID groupId, ScheduleStatus scheduleStatus);

    List<GroupSchedule> findByCoachIdAndStatusAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
            UUID coachId,
            ScheduleStatus status,
            LocalDate from,
            LocalDate to
    );

    @Query("""
        select s from GroupSchedule s
        where s.groupId = :groupId
          and s.coachId = :coachId
          and s.scheduleType = :type
          and s.startDate = :startDate
          and s.endDate = :endDate
          and s.status = :status
    """)
    List<GroupSchedule> findBatch(
            UUID groupId,
            UUID coachId,
            ScheduleType type,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleStatus status
    );

    @Query("""
        select count(distinct gs.dayOfWeek)
        from GroupSchedule gs
        where gs.groupId = :groupId
          and gs.status = 'ACTIVE'
    """)
    int countWeeklySessions(UUID groupId);

    boolean existsByGroupIdAndStatus(UUID groupId, ScheduleStatus status);
}
