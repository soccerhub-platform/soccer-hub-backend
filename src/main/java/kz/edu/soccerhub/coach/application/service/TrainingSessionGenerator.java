package kz.edu.soccerhub.coach.application.service;

import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TrainingSessionGenerator {

    private final GroupScheduleRepository groupScheduleRepository;
    private final TrainingSessionRepository trainingSessionRepository;

    @Transactional
    public List<TrainingSession> ensureSessionsForDate(UUID coachId, LocalDate date) {
        List<GroupSchedule> schedules = groupScheduleRepository.findCoachSchedulesForDate(
                coachId,
                date,
                date.getDayOfWeek()
        );

        List<TrainingSession> sessions = new ArrayList<>(schedules.size());
        for (GroupSchedule schedule : schedules) {
            sessions.add(ensureSessionFromSchedule(schedule, date));
        }

        return sessions;
    }

    @Transactional
    public void ensureSessionsForRange(UUID coachId, LocalDate from, LocalDate to) {
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            ensureSessionsForDate(coachId, cursor);
            cursor = cursor.plusDays(1);
        }
    }

    private TrainingSession ensureSessionFromSchedule(GroupSchedule schedule, LocalDate date) {
        return trainingSessionRepository.findByScheduleIdAndSessionDate(schedule.getId(), date)
                .orElseGet(() -> createSession(schedule, date));
    }

    private TrainingSession createSession(GroupSchedule schedule, LocalDate date) {
        UUID resolvedCoachId = resolveCoachForSession(schedule);

        TrainingSession newSession = TrainingSession.builder()
                .id(UUID.randomUUID())
                .groupId(schedule.getGroupId())
                .coachId(resolvedCoachId)
                .scheduleId(schedule.getId())
                .locationId(schedule.getLocationId())
                .sessionDate(date)
                .scheduledStartAt(LocalDateTime.of(date, schedule.getStartTime()))
                .scheduledEndAt(LocalDateTime.of(date, schedule.getEndTime()))
                .status(TrainingSessionStatus.PLANNED)
                .reportDone(false)
                .build();

        try {
            return trainingSessionRepository.save(newSession);
        } catch (DataIntegrityViolationException ignored) {
            return trainingSessionRepository.findByScheduleIdAndSessionDate(schedule.getId(), date)
                    .orElseThrow();
        }
    }

    private UUID resolveCoachForSession(GroupSchedule schedule) {
        if (schedule.isSubstitution() && schedule.getSubstitutionCoachId() != null) {
            return schedule.getSubstitutionCoachId();
        }
        return schedule.getCoachId();
    }
}
