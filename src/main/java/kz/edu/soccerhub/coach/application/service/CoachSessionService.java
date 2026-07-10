package kz.edu.soccerhub.coach.application.service;

import kz.edu.soccerhub.coach.application.dto.session.*;
import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.TrainingSessionAttendance;
import kz.edu.soccerhub.coach.domain.model.enums.SessionReportStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.CoachProfileRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionAttendanceRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.ForbiddenException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.organization.domain.model.Group;
import kz.edu.soccerhub.organization.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachSessionService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String DEFAULT_TIMEZONE = "Asia/Almaty";
    private static final int REPORT_DEADLINE_HOURS = 24;

    private final CoachProfileRepository coachProfileRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingSessionAttendanceRepository trainingSessionAttendanceRepository;
    private final GroupRepository groupRepository;
    private final CoachRosterReader coachRosterReader;

    @Transactional
    public CoachTodaySessionsResponse getTodaySessions(UUID currentUserId, LocalDate date, String timezone) {
        ZoneId zoneId = validateZone(timezone);
        ensureCoachProfile(currentUserId);

        List<TrainingSession> sessions =
                trainingSessionRepository.findByCoachIdAndSessionDateOrderByScheduledStartAtAsc(currentUserId, date);

        Map<UUID, String> groupNames = getGroupNames(sessions);

        List<CoachTodaySessionItem> items = sessions.stream()
                .map(session -> {
                    List<CoachRosterReader.ActivePlayerView> players =
                            coachRosterReader.getActivePlayersByGroupAndDate(session.getGroupId(), session.getSessionDate());
                    Set<UUID> playerIds = players.stream().map(CoachRosterReader.ActivePlayerView::id).collect(Collectors.toSet());
                    String summary = calculateAttendanceSummary(session.getId(), playerIds);
                    return new CoachTodaySessionItem(
                            session.getId(),
                            groupNames.getOrDefault(session.getGroupId(), "Unknown group"),
                            session.getSessionDate(),
                            session.getScheduledStartAt().toLocalTime().format(TIME_FORMAT),
                            players.size(),
                            toResponseStatus(session, zoneId),
                            session.getCancelReason(),
                            session.isReportDone(),
                            resolveReportStatus(session, zoneId).name(),
                            reportDeadline(session),
                            submittedAt(session),
                            summary
                    );
                })
                .toList();

        return new CoachTodaySessionsResponse(date, timezone, items);
    }

    @Transactional(readOnly = true)
    public CoachSessionDetailsResponse getSessionDetails(UUID currentUserId, UUID sessionId, String timezone) {
        ZoneId zoneId = validateZone(timezone);
        ensureCoachProfile(currentUserId);

        TrainingSession session = getCoachSession(sessionId, currentUserId);
        String groupName = groupRepository.findById(session.getGroupId()).map(Group::getName).orElse("Unknown group");

        List<CoachRosterReader.ActivePlayerView> players =
                coachRosterReader.getActivePlayersByGroupAndDate(session.getGroupId(), session.getSessionDate());
        Set<UUID> playerIds = players.stream().map(CoachRosterReader.ActivePlayerView::id).collect(Collectors.toSet());

        Map<UUID, TrainingSessionAttendance> attendanceByPlayer =
                trainingSessionAttendanceRepository.findBySessionId(session.getId()).stream()
                        .collect(Collectors.toMap(TrainingSessionAttendance::getPlayerId, Function.identity()));

        List<CoachSessionStudentItem> students = players.stream()
                .map(player -> {
                    TrainingSessionAttendance attendance = attendanceByPlayer.get(player.id());
                    return new CoachSessionStudentItem(
                            player.id(),
                            formatPlayerName(player.firstName(), player.lastName()),
                            attendance == null ? null : attendance.getStatus().name()
                    );
                })
                .toList();

        return new CoachSessionDetailsResponse(
                session.getId(),
                groupName,
                session.getSessionDate(),
                session.getScheduledStartAt().toLocalTime().format(TIME_FORMAT),
                toResponseStatus(session, zoneId),
                session.getCancelReason(),
                session.isReportDone(),
                resolveReportStatus(session, zoneId).name(),
                reportDeadline(session),
                submittedAt(session),
                calculateAttendanceSummary(session.getId(), playerIds),
                students,
                new CoachSessionReportView(
                        session.getTopic(),
                        session.getCoachComment(),
                        session.getIncidents(),
                        session.getHomework()
                )
        );
    }

    @Transactional
    public CoachScheduleResponse getSchedule(UUID currentUserId, LocalDate dateFrom, LocalDate dateTo, String timezone) {
        ZoneId zoneId = validateZone(timezone);
        validateDateRange(dateFrom, dateTo);
        ensureCoachProfile(currentUserId);

        long days = dateTo.toEpochDay() - dateFrom.toEpochDay() + 1;
        if (days > 31) {
            throw new BadRequestException("Schedule range cannot exceed 31 days", dateFrom, dateTo);
        }

        List<TrainingSession> sessions = trainingSessionRepository
                .findByCoachIdAndSessionDateBetweenOrderBySessionDateAscScheduledStartAtAsc(currentUserId, dateFrom, dateTo);

        Map<UUID, String> groupNames = getGroupNames(sessions);
        Map<LocalDate, List<CoachScheduleSessionItem>> grouped = new LinkedHashMap<>();

        for (TrainingSession session : sessions) {
            grouped.computeIfAbsent(session.getSessionDate(), ignored -> new ArrayList<>())
                    .add(new CoachScheduleSessionItem(
                            session.getId(),
                            session.getScheduledStartAt().toLocalTime().format(TIME_FORMAT),
                            groupNames.getOrDefault(session.getGroupId(), "Unknown group"),
                            toResponseStatus(session, zoneId),
                            resolveReportStatus(session, zoneId).name(),
                            reportDeadline(session),
                            submittedAt(session)
                    ));
        }

        List<CoachScheduleDayItem> daysItems = grouped.entrySet().stream()
                .map(it -> new CoachScheduleDayItem(it.getKey(), it.getValue()))
                .toList();

        return new CoachScheduleResponse(timezone, daysItems);
    }

    @Transactional(readOnly = true)
    public CoachHistoryResponse getHistory(
            UUID currentUserId,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        validateDateRange(dateFrom, dateTo);
        ensureCoachProfile(currentUserId);

        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Invalid pagination params", page, size);
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "sessionDate", "scheduledStartAt")
        );

        Page<TrainingSession> sessions =
                trainingSessionRepository.findByCoachIdAndSessionDateBetween(currentUserId, dateFrom, dateTo, pageable);
        Map<UUID, String> groupNames = getGroupNames(sessions.getContent());

        ZoneId zoneId = validateZone(DEFAULT_TIMEZONE);

        List<CoachHistorySessionItem> content = sessions.getContent().stream()
                .map(session -> {
                    Set<UUID> playerIds = coachRosterReader.getActivePlayersByGroupAndDate(
                                    session.getGroupId(),
                                    session.getSessionDate()
                            ).stream()
                            .map(CoachRosterReader.ActivePlayerView::id)
                            .collect(Collectors.toSet());

                    return new CoachHistorySessionItem(
                            session.getId(),
                            session.getSessionDate(),
                            groupNames.getOrDefault(session.getGroupId(), "Unknown group"),
                            toResponseStatus(session, zoneId),
                            calculateAttendanceSummary(session.getId(), playerIds),
                            session.isReportDone(),
                            resolveReportStatus(session, zoneId).name(),
                            reportDeadline(session),
                            submittedAt(session)
                    );
                })
                .toList();

        return new CoachHistoryResponse(
                content,
                sessions.getTotalElements(),
                sessions.getTotalPages(),
                sessions.getNumber(),
                sessions.getSize()
        );
    }

    @Transactional
    public CoachAttendanceUpdateResponse updateAttendance(
            UUID currentUserId,
            UUID sessionId,
            CoachAttendancePatchInput input
    ) {
        ensureCoachProfile(currentUserId);
        TrainingSession session = getCoachSession(sessionId, currentUserId);
        ZoneId zoneId = validateZone(DEFAULT_TIMEZONE);
        if (!isInProgressOrOverdue(session, zoneId)) {
            throw new BadRequestException("Attendance can be updated only for IN_PROGRESS or OVERDUE sessions", sessionId);
        }

        List<CoachRosterReader.ActivePlayerView> activePlayers =
                coachRosterReader.getActivePlayersByGroupAndDate(session.getGroupId(), session.getSessionDate());
        Set<UUID> activePlayerIds = activePlayers.stream().map(CoachRosterReader.ActivePlayerView::id).collect(Collectors.toSet());

        Set<UUID> duplicateCheck = new HashSet<>();
        for (CoachAttendanceStudentInput student : input.students()) {
            if (!duplicateCheck.add(student.studentId())) {
                throw new BadRequestException("Duplicate student in attendance patch", student.studentId());
            }
            if (!activePlayerIds.contains(student.studentId())) {
                throw new BadRequestException("Student does not belong to active group roster", student.studentId());
            }
        }

        Map<UUID, TrainingSessionAttendance> existing = trainingSessionAttendanceRepository.findBySessionId(sessionId)
                .stream()
                .collect(Collectors.toMap(TrainingSessionAttendance::getPlayerId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        List<TrainingSessionAttendance> toSave = new ArrayList<>();
        for (CoachAttendanceStudentInput studentInput : input.students()) {
            TrainingSessionAttendance item = existing.get(studentInput.studentId());
            if (item == null) {
                item = TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .playerId(studentInput.studentId())
                        .build();
            }
            item.setStatus(studentInput.attendance());
            item.setMarkedBy(currentUserId);
            item.setMarkedAt(now);
            toSave.add(item);
        }

        trainingSessionAttendanceRepository.saveAll(toSave);
        return new CoachAttendanceUpdateResponse(true, calculateAttendanceSummary(sessionId, activePlayerIds));
    }

    @Transactional
    public CoachAttendanceUpdateResponse markAllPresent(UUID currentUserId, UUID sessionId) {
        ensureCoachProfile(currentUserId);
        TrainingSession session = getCoachSession(sessionId, currentUserId);
        ZoneId zoneId = validateZone(DEFAULT_TIMEZONE);
        if (!isInProgressOrOverdue(session, zoneId)) {
            throw new BadRequestException("Attendance can be updated only for IN_PROGRESS or OVERDUE sessions", sessionId);
        }

        List<CoachRosterReader.ActivePlayerView> activePlayers =
                coachRosterReader.getActivePlayersByGroupAndDate(session.getGroupId(), session.getSessionDate());
        Set<UUID> activePlayerIds = activePlayers.stream().map(CoachRosterReader.ActivePlayerView::id).collect(Collectors.toSet());

        Map<UUID, TrainingSessionAttendance> existing = trainingSessionAttendanceRepository.findBySessionId(sessionId)
                .stream()
                .collect(Collectors.toMap(TrainingSessionAttendance::getPlayerId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        List<TrainingSessionAttendance> toSave = new ArrayList<>();
        for (UUID playerId : activePlayerIds) {
            TrainingSessionAttendance item = existing.get(playerId);
            if (item == null) {
                item = TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .playerId(playerId)
                        .build();
            }
            item.setStatus(TrainingSessionAttendanceStatus.PRESENT);
            item.setMarkedBy(currentUserId);
            item.setMarkedAt(now);
            toSave.add(item);
        }

        trainingSessionAttendanceRepository.saveAll(toSave);
        return new CoachAttendanceUpdateResponse(true, calculateAttendanceSummary(sessionId, activePlayerIds));
    }

    @Transactional
    public CoachReportSaveResponse saveReport(UUID currentUserId, UUID sessionId, CoachReportSaveInput input) {
        ensureCoachProfile(currentUserId);
        TrainingSession session = getCoachSession(sessionId, currentUserId);
        ZoneId zoneId = validateZone(DEFAULT_TIMEZONE);

        boolean canSave = session.getStatus() == TrainingSessionStatus.IN_PROGRESS
                          || (session.getStatus() == TrainingSessionStatus.PLANNED && isOverdue(session, zoneId));
        if (!canSave) {
            throw new BadRequestException("Report can be saved only for IN_PROGRESS or OVERDUE session", sessionId);
        }

        session.setTopic(input.topic().trim());
        session.setCoachComment(input.coachComment());
        session.setIncidents(input.incidents());
        session.setHomework(input.homework());
        session.setReportDone(!input.topic().trim().isEmpty());

        return new CoachReportSaveResponse(true, session.isReportDone());
    }

    @Transactional
    public CoachSimpleStatusResponse startSession(UUID currentUserId, UUID sessionId) {
        ensureCoachProfile(currentUserId);
        TrainingSession session = getCoachSession(sessionId, currentUserId);

        if (session.getStatus() != TrainingSessionStatus.PLANNED) {
            throw new BadRequestException("Transition not allowed", session.getStatus(), TrainingSessionStatus.IN_PROGRESS);
        }

        session.setStatus(TrainingSessionStatus.IN_PROGRESS);
        session.setActualStartAt(LocalDateTime.now());
        session.setStartedBy(currentUserId);

        return new CoachSimpleStatusResponse(true, session.getStatus().name());
    }

    @Transactional
    public CoachSimpleStatusResponse completeSession(UUID currentUserId, UUID sessionId) {
        ensureCoachProfile(currentUserId);
        TrainingSession session = getCoachSession(sessionId, currentUserId);
        ZoneId zoneId = validateZone(DEFAULT_TIMEZONE);
        if (!isInProgressOrOverdue(session, zoneId)) {
            throw new BadRequestException("Transition not allowed", session.getStatus(), "IN_PROGRESS_OR_OVERDUE");
        }

        if (!session.isReportDone() || session.getTopic() == null || session.getTopic().isBlank()) {
            throw new BadRequestException("Report is required before completion", sessionId);
        }

        Set<UUID> activePlayerIds = coachRosterReader.getActivePlayersByGroupAndDate(
                        session.getGroupId(),
                        session.getSessionDate()
                ).stream()
                .map(CoachRosterReader.ActivePlayerView::id)
                .collect(Collectors.toSet());

        Set<UUID> attendancePlayerIds = trainingSessionAttendanceRepository.findBySessionId(sessionId)
                .stream()
                .map(TrainingSessionAttendance::getPlayerId)
                .collect(Collectors.toSet());

        if (!attendancePlayerIds.containsAll(activePlayerIds)) {
            throw new BadRequestException("Attendance is incomplete", sessionId);
        }

        session.setStatus(TrainingSessionStatus.COMPLETED);
        session.setActualEndAt(LocalDateTime.now());
        session.setCompletedBy(currentUserId);

        return new CoachSimpleStatusResponse(true, session.getStatus().name());
    }

    @Transactional
    public CoachSimpleStatusResponse cancelSession(UUID currentUserId, UUID sessionId, CoachCancelSessionInput input) {
        ensureCoachProfile(currentUserId);
        TrainingSession session = getCoachSession(sessionId, currentUserId);
        ZoneId zoneId = validateZone(DEFAULT_TIMEZONE);

        if (isOverdue(session, zoneId)) {
            throw new BadRequestException("Transition not allowed for OVERDUE session", sessionId);
        }

        if (session.getStatus() != TrainingSessionStatus.PLANNED && session.getStatus() != TrainingSessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Transition not allowed", session.getStatus(), TrainingSessionStatus.CANCELLED);
        }

        session.setStatus(TrainingSessionStatus.CANCELLED);
        session.setCancelReason(input.reason());
        session.setCancelledBy(currentUserId);

        return new CoachSimpleStatusResponse(true, session.getStatus().name());
    }

    private String calculateAttendanceSummary(UUID sessionId, Set<UUID> activePlayerIds) {
        if (activePlayerIds.isEmpty()) {
            return "0/0";
        }

        long presentLike = trainingSessionAttendanceRepository.findBySessionId(sessionId).stream()
                .filter(item -> activePlayerIds.contains(item.getPlayerId()))
                .filter(item -> item.getStatus() == TrainingSessionAttendanceStatus.PRESENT
                                || item.getStatus() == TrainingSessionAttendanceStatus.LATE)
                .count();

        return presentLike + "/" + activePlayerIds.size();
    }

    private Map<UUID, String> getGroupNames(List<TrainingSession> sessions) {
        Set<UUID> groupIds = sessions.stream().map(TrainingSession::getGroupId).collect(Collectors.toSet());
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        return groupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(Group::getId, Group::getName));
    }

    private TrainingSession getCoachSession(UUID sessionId, UUID coachId) {
        return trainingSessionRepository.findByIdAndCoachId(sessionId, coachId)
                .orElseThrow(() -> new NotFoundException("Session not found", sessionId));
    }

    private void ensureCoachProfile(UUID currentUserId) {
        if (!coachProfileRepository.existsById(currentUserId)) {
            throw new ForbiddenException("Coach profile not found");
        }
    }

    private void ensureStatus(TrainingSession session, TrainingSessionStatus required, String message) {
        if (session.getStatus() != required) {
            throw new BadRequestException(message, session.getStatus(), required);
        }
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null) {
            throw new BadRequestException("dateFrom and dateTo are required", dateFrom, dateTo);
        }
        if (dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom cannot be after dateTo", dateFrom, dateTo);
        }
    }

    private ZoneId validateZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid timezone", timezone);
        }
    }

    private String toResponseStatus(TrainingSession session, ZoneId zoneId) {
        if (isOverdue(session, zoneId)) {
            return "OVERDUE";
        }
        return session.getStatus().name();
    }

    private SessionReportStatus resolveReportStatus(TrainingSession session, ZoneId zoneId) {
        if (session.isReportDone()) {
            return SessionReportStatus.SUBMITTED;
        }
        if (session.getScheduledEndAt() == null || session.getScheduledEndAt().isAfter(LocalDateTime.now(zoneId))) {
            return SessionReportStatus.NOT_REQUIRED;
        }
        return LocalDateTime.now(zoneId).isAfter(reportDeadline(session))
                ? SessionReportStatus.OVERDUE
                : SessionReportStatus.PENDING;
    }

    private LocalDateTime reportDeadline(TrainingSession session) {
        return session.getScheduledEndAt() == null ? null : session.getScheduledEndAt().plusHours(REPORT_DEADLINE_HOURS);
    }

    private LocalDateTime submittedAt(TrainingSession session) {
        return session.isReportDone() ? session.getUpdatedAt() : null;
    }

    private boolean isOverdue(TrainingSession session, ZoneId zoneId) {
        if (session.getStatus() != TrainingSessionStatus.PLANNED && session.getStatus() != TrainingSessionStatus.IN_PROGRESS) {
            return false;
        }
        return session.getScheduledEndAt().isBefore(LocalDateTime.now(zoneId));
    }

    private boolean isInProgressOrOverdue(TrainingSession session, ZoneId zoneId) {
        return session.getStatus() == TrainingSessionStatus.IN_PROGRESS || isOverdue(session, zoneId);
    }

    private String formatPlayerName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName.charAt(0) + ".";
    }
}
