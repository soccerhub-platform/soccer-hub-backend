package kz.edu.soccerhub.coach.application.service;

import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.common.port.TrainingSessionPlanningPort;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingSessionMaterializer implements TrainingSessionPlanningPort {

    private static final int MATERIALIZATION_HORIZON_DAYS = 90;
    private static final String RESCHEDULED_REASON = "Schedule changed";
    private static final String SCHEDULE_CANCELLED_REASON = "Schedule cancelled";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Almaty");

    private final GroupScheduleRepository groupScheduleRepository;
    private final TrainingSessionRepository trainingSessionRepository;

    @Override
    @Transactional
    public void materializeSchedules(Collection<UUID> scheduleIds) {
        materializeSchedules(scheduleIds, LocalDate.now(), horizonEnd());
    }

    @Override
    @Transactional
    public void resyncSchedules(Collection<UUID> oldScheduleIds, Collection<UUID> newScheduleIds, LocalDate fromDate) {
        cancelFuturePlannedSessions(oldScheduleIds, fromDate, RESCHEDULED_REASON);
        materializeSchedules(newScheduleIds, fromDate, horizonEnd());
    }

    @Override
    @Transactional
    public void cancelFuturePlannedSessions(Collection<UUID> scheduleIds, LocalDate fromDate, String reason) {
        Set<UUID> ids = normalizeIds(scheduleIds);
        if (ids.isEmpty()) {
            return;
        }
        trainingSessionRepository.cancelFuturePlannedByScheduleIds(ids, fromDate, reason);
    }

    @Override
    @Transactional
    public void reactivateScheduleCancelledSessions(Collection<UUID> scheduleIds, LocalDate fromDate) {
        Set<UUID> ids = normalizeIds(scheduleIds);
        if (ids.isEmpty()) {
            return;
        }
        trainingSessionRepository.reactivateScheduleCancelledByScheduleIds(
                ids,
                fromDate,
                SCHEDULE_CANCELLED_REASON
        );
    }

    @Override
    @Transactional
    public int replaceCoachInFuturePlannedSessions(
            UUID groupId,
            UUID currentCoachId,
            UUID replacementCoachId,
            LocalDate fromDate
    ) {
        return trainingSessionRepository.replaceCoachInFuturePlannedSessions(
                groupId,
                currentCoachId,
                replacementCoachId,
                fromDate
        );
    }

    @Override
    @Transactional
    public int replaceScheduleInFuturePlannedSessions(
            UUID currentScheduleId,
            UUID replacementScheduleId,
            LocalDate fromDate
    ) {
        return trainingSessionRepository.replaceScheduleInFuturePlannedSessions(
                currentScheduleId,
                replacementScheduleId,
                fromDate
        );
    }

    @Override
    @Scheduled(cron = "0 10 3 * * *", zone = "Asia/Almaty")
    @Transactional
    public void materializeAllActiveSchedules() {
        LocalDate from = today();
        LocalDate to = horizonEnd();
        List<GroupSchedule> schedules = groupScheduleRepository.findActiveSchedulesBetween(from, to);
        for (GroupSchedule schedule : schedules) {
            materializeSchedule(schedule, from, to);
        }
    }

    private void materializeSchedules(Collection<UUID> scheduleIds, LocalDate fromDate, LocalDate toDate) {
        Set<UUID> ids = normalizeIds(scheduleIds);
        if (ids.isEmpty()) {
            return;
        }
        groupScheduleRepository.findAllById(ids).stream()
                .filter(schedule -> schedule.getStatus() == ScheduleStatus.ACTIVE)
                .forEach(schedule -> materializeSchedule(schedule, fromDate, toDate));
    }

    private void materializeSchedule(GroupSchedule schedule, LocalDate fromDate, LocalDate toDate) {
        LocalDate cursor = max(fromDate, schedule.getStartDate());
        LocalDate end = min(toDate, schedule.getEndDate());
        while (!cursor.isAfter(end)) {
            if (cursor.getDayOfWeek() == schedule.getDayOfWeek()) {
                ensureSessionFromSchedule(schedule, cursor);
            }
            cursor = cursor.plusDays(1);
        }
    }

    private void ensureSessionFromSchedule(GroupSchedule schedule, LocalDate date) {
        if (trainingSessionRepository.findByScheduleIdAndSessionDate(schedule.getId(), date).isPresent()) {
            return;
        }

        LocalDateTime scheduledStartAt = LocalDateTime.of(date, schedule.getStartTime());
        boolean slotExists = trainingSessionRepository.existsByGroupIdAndSessionDateAndScheduledStartAtAndStatusNot(
                schedule.getGroupId(),
                date,
                scheduledStartAt,
                TrainingSessionStatus.CANCELLED
        );
        if (slotExists) {
            return;
        }

        try {
            trainingSessionRepository.save(buildSession(schedule, date));
        } catch (DataIntegrityViolationException ignored) {
            // Concurrent materialization can race on unique indexes. The existing row is good enough.
        }
    }

    private TrainingSession buildSession(GroupSchedule schedule, LocalDate date) {
        return TrainingSession.builder()
                .id(UUID.randomUUID())
                .groupId(schedule.getGroupId())
                .coachId(resolveCoachForSession(schedule))
                .scheduleId(schedule.getId())
                .locationId(schedule.getLocationId())
                .sessionDate(date)
                .scheduledStartAt(LocalDateTime.of(date, schedule.getStartTime()))
                .scheduledEndAt(LocalDateTime.of(date, schedule.getEndTime()))
                .status(TrainingSessionStatus.PLANNED)
                .reportDone(false)
                .build();
    }

    private UUID resolveCoachForSession(GroupSchedule schedule) {
        if (schedule.isSubstitution() && schedule.getSubstitutionCoachId() != null) {
            return schedule.getSubstitutionCoachId();
        }
        return schedule.getCoachId();
    }

    private Set<UUID> normalizeIds(Collection<UUID> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return Set.of();
        }
        return scheduleIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    private LocalDate horizonEnd() {
        return today().plusDays(MATERIALIZATION_HORIZON_DAYS);
    }

    private LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }

    private LocalDate max(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }
}
