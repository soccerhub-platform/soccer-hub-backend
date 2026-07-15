package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.session.AdminGroupSessionsOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminCancelSessionInput;
import kz.edu.soccerhub.admin.application.dto.session.AdminGroupAttendanceOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminRescheduleSessionInput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSessionAttendanceOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSessionAttendanceUpdateInput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSessionDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSubstituteCoachInput;
import kz.edu.soccerhub.coach.application.service.CoachRosterReader;
import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.TrainingSessionAttendance;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionAttendanceRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupActivityPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.MediaAccessPort;
import kz.edu.soccerhub.common.port.MediaAvatarPort;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import kz.edu.soccerhub.organization.domain.model.Location;
import kz.edu.soccerhub.organization.domain.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private static final long MAX_RANGE_DAYS = 31;
    private static final String ACTIVITY_SESSION_CANCELLED = "SESSION_CANCELLED";
    private static final String ACTIVITY_SESSION_RESCHEDULED = "SESSION_RESCHEDULED";
    private static final String ACTIVITY_SESSION_COACH_SUBSTITUTED = "SESSION_COACH_SUBSTITUTED";
    private static final String ACTIVITY_ATTENDANCE_UPDATED = "ATTENDANCE_UPDATED";

    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final GroupPort groupPort;
    private final GroupCoachPort groupCoachPort;
    private final CoachPort coachPort;
    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingSessionAttendanceRepository trainingSessionAttendanceRepository;
    private final CoachRosterReader coachRosterReader;
    private final LocationRepository locationRepository;
    private final GroupActivityPort groupActivityPort;
    private final MediaAvatarPort mediaAvatarPort;
    private final MediaAccessPort mediaAccessPort;

    @Transactional(readOnly = true)
    public AdminGroupSessionsOutput getGroupSessions(
            UUID adminId,
            UUID groupId,
            LocalDate from,
            LocalDate to,
            String status,
            UUID coachId
    ) {
        verifyAdmin(adminId);
        GroupDto group = groupPort.getGroupById(groupId);
        verifyAdminBranchAccess(adminId, group.branchId());
        validateDateRange(from, to);

        List<TrainingSession> sessions = trainingSessionRepository
                .findByGroupIdAndSessionDateBetweenOrderBySessionDateAscScheduledStartAtAsc(groupId, from, to);

        if (coachId != null) {
            sessions = sessions.stream()
                    .filter(session -> coachId.equals(session.getCoachId()))
                    .toList();
        }
        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim().toUpperCase();
            sessions = sessions.stream()
                    .filter(session -> normalizedStatus.equals(resolveEffectiveStatus(session)))
                    .toList();
        }
        if (sessions.isEmpty()) {
            return new AdminGroupSessionsOutput(groupId, from, to, List.of());
        }

        Map<UUID, CoachDto> coachById = getCoachMap(sessions.stream()
                .map(TrainingSession::getCoachId)
                .collect(Collectors.toSet()));
        Map<UUID, String> locationNames = getLocationNames(sessions.stream()
                .map(TrainingSession::getLocationId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<UUID, GroupCoachDto> groupCoachByCoachId = groupCoachPort.getActiveCoaches(groupId).stream()
                .collect(Collectors.toMap(GroupCoachDto::coachId, Function.identity(), (left, right) -> left));
        Map<UUID, List<TrainingSessionAttendance>> attendanceBySessionId = trainingSessionAttendanceRepository.findBySessionIdIn(
                        sessions.stream().map(TrainingSession::getId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.groupingBy(TrainingSessionAttendance::getSessionId));

        List<AdminGroupSessionsOutput.SessionItem> items = sessions.stream()
                .map(session -> toSessionItem(session, coachById, groupCoachByCoachId, attendanceBySessionId, locationNames))
                .toList();

        return new AdminGroupSessionsOutput(groupId, from, to, items);
    }

    @Transactional(readOnly = true)
    public AdminGroupAttendanceOutput getGroupAttendance(
            UUID adminId,
            UUID groupId,
            LocalDate from,
            LocalDate to
    ) {
        verifyAdmin(adminId);
        GroupDto group = groupPort.getGroupById(groupId);
        verifyAdminBranchAccess(adminId, group.branchId());
        validateDateRange(from, to);

        List<TrainingSession> sessions = trainingSessionRepository
                .findByGroupIdAndSessionDateBetweenOrderBySessionDateAscScheduledStartAtAsc(groupId, from, to);

        if (sessions.isEmpty()) {
            return new AdminGroupAttendanceOutput(
                    groupId,
                    from,
                    to,
                    new AdminGroupAttendanceOutput.Summary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                    List.of()
            );
        }

        Map<UUID, List<TrainingSessionAttendance>> attendanceBySessionId = trainingSessionAttendanceRepository.findBySessionIdIn(
                        sessions.stream().map(TrainingSession::getId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.groupingBy(TrainingSessionAttendance::getSessionId));

        List<AdminGroupAttendanceOutput.SessionItem> items = sessions.stream()
                .map(session -> {
                    int participantsCount = coachRosterReader
                            .getActivePlayersByGroupAndDate(session.getGroupId(), session.getSessionDate())
                            .size();
                    AdminSessionAttendanceOutput.Summary summary = toDetailedAttendanceSummary(
                            attendanceBySessionId.getOrDefault(session.getId(), List.of()),
                            participantsCount
                    );
                    return new AdminGroupAttendanceOutput.SessionItem(
                            session.getId(),
                            session.getSessionDate(),
                            session.getScheduledStartAt(),
                            session.getScheduledEndAt(),
                            session.getStatus().name(),
                            resolveEffectiveStatus(session),
                            summary,
                            new AdminGroupAttendanceOutput.Capabilities(
                                    canOpenAttendance(session),
                                    canEditAttendance(session)
                            )
                    );
                })
                .toList();

        int sessionsCount = items.size();
        int recordedSessionsCount = (int) items.stream()
                .filter(item -> item.summary().marked() > 0)
                .count();
        int totalParticipants = items.stream().mapToInt(item -> item.summary().total()).sum();
        int totalMarked = items.stream().mapToInt(item -> item.summary().marked()).sum();
        int totalPresent = items.stream().mapToInt(item -> item.summary().present()).sum();
        int totalAbsent = items.stream().mapToInt(item -> item.summary().absent()).sum();
        int totalExcused = items.stream().mapToInt(item -> item.summary().excused()).sum();
        int totalLate = items.stream().mapToInt(item -> item.summary().late()).sum();
        int totalUnmarked = items.stream().mapToInt(item -> item.summary().unmarked()).sum();
        int totalPresentLike = items.stream().mapToInt(item -> item.summary().presentLike()).sum();
        int averageAttendanceRate = totalParticipants == 0
                ? 0
                : (int) Math.round((double) totalPresentLike * 100 / totalParticipants);

        return new AdminGroupAttendanceOutput(
                groupId,
                from,
                to,
                new AdminGroupAttendanceOutput.Summary(
                        sessionsCount,
                        recordedSessionsCount,
                        totalParticipants,
                        totalMarked,
                        totalPresent,
                        totalAbsent,
                        totalExcused,
                        totalLate,
                        totalUnmarked,
                        totalPresentLike,
                        averageAttendanceRate
                ),
                items
        );
    }

    @Transactional(readOnly = true)
    public AdminSessionDetailsOutput getSessionDetails(UUID adminId, UUID sessionId) {
        verifyAdmin(adminId);

        TrainingSession session = trainingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found", sessionId));
        GroupDto group = groupPort.getGroupById(session.getGroupId());
        verifyAdminBranchAccess(adminId, group.branchId());

        Map<UUID, CoachDto> coachById = getCoachMap(Set.of(session.getCoachId()));
        Map<UUID, String> locationNames = getLocationNames(session.getLocationId() == null ? Set.of() : Set.of(session.getLocationId()));
        Map<UUID, GroupCoachDto> groupCoachByCoachId = groupCoachPort.getActiveCoaches(session.getGroupId()).stream()
                .collect(Collectors.toMap(GroupCoachDto::coachId, Function.identity(), (left, right) -> left));

        int participantsCount = coachRosterReader.getActivePlayersByGroupAndDate(session.getGroupId(), session.getSessionDate()).size();
        List<TrainingSessionAttendance> attendanceEntries = trainingSessionAttendanceRepository.findBySessionId(sessionId);
        AdminGroupSessionsOutput.AttendanceSummary attendance = toAttendanceSummary(attendanceEntries, participantsCount);

        return new AdminSessionDetailsOutput(
                session.getId(),
                new AdminSessionDetailsOutput.GroupRef(group.groupId(), group.name(), getGroupAvatar(group.groupId())),
                session.getScheduleId(),
                session.getSessionDate(),
                session.getScheduledStartAt(),
                session.getScheduledEndAt(),
                session.getActualStartAt(),
                session.getActualEndAt(),
                session.getStatus().name(),
                resolveEffectiveStatus(session),
                session.getCancelReason(),
                toLocationRef(session.getLocationId(), locationNames),
                List.of(toCoachRef(session.getCoachId(), coachById, groupCoachByCoachId)),
                participantsCount,
                attendance,
                buildCapabilities(session)
        );
    }

    @Transactional(readOnly = true)
    public AdminSessionAttendanceOutput getSessionAttendance(UUID adminId, UUID sessionId) {
        TrainingSession session = getManagedSession(adminId, sessionId);
        GroupDto group = groupPort.getGroupById(session.getGroupId());

        List<CoachRosterReader.ActivePlayerView> activePlayers =
                coachRosterReader.getActivePlayersByGroupAndDate(session.getGroupId(), session.getSessionDate());
        Map<UUID, TrainingSessionAttendance> attendanceByPlayerId = trainingSessionAttendanceRepository.findBySessionId(sessionId)
                .stream()
                .collect(Collectors.toMap(TrainingSessionAttendance::getPlayerId, Function.identity()));

        List<AdminSessionAttendanceOutput.ParticipantItem> participants = activePlayers.stream()
                .sorted(Comparator.comparing(player -> StreamUtil.joinNonBlank(player.firstName(), player.lastName())))
                .map(player -> {
                    TrainingSessionAttendance attendance = attendanceByPlayerId.get(player.id());
                    return new AdminSessionAttendanceOutput.ParticipantItem(
                            player.id(),
                            StreamUtil.joinNonBlank(player.firstName(), player.lastName()),
                            attendance == null ? null : attendance.getStatus(),
                            attendance == null ? null : attendance.getComment()
                    );
                })
                .toList();

        return new AdminSessionAttendanceOutput(
                session.getId(),
                new AdminSessionAttendanceOutput.GroupRef(group.groupId(), group.name(), getGroupAvatar(group.groupId())),
                session.getSessionDate(),
                session.getScheduledStartAt(),
                session.getScheduledEndAt(),
                session.getStatus().name(),
                resolveEffectiveStatus(session),
                toDetailedAttendanceSummary(attendanceByPlayerId.values(), participants.size()),
                participants,
                new AdminSessionAttendanceOutput.Capabilities(canEditAttendance(session))
        );
    }

    @Transactional
    public AdminSessionDetailsOutput cancelSession(UUID adminId, UUID sessionId, AdminCancelSessionInput input) {
        TrainingSession session = getManagedSession(adminId, sessionId);
        if (!canCancel(session)) {
            throw new BadRequestException("Session cannot be cancelled", sessionId, session.getStatus().name());
        }
        session.setStatus(TrainingSessionStatus.CANCELLED);
        session.setCancelReason(buildCancelReason(input.reasonCode(), input.comment()));
        session.setCancelledBy(adminId);
        groupActivityPort.recordGroupActivity(
                session.getGroupId(),
                adminId,
                ACTIVITY_SESSION_CANCELLED,
                activityPayload()
                        .put("sessionId", session.getId())
                        .put("sessionDate", session.getSessionDate())
                        .put("startsAt", session.getScheduledStartAt())
                        .put("endsAt", session.getScheduledEndAt())
                        .put("reasonCode", input.reasonCode())
                        .put("comment", normalizeComment(input.comment()))
                        .build()
        );
        return getSessionDetails(adminId, sessionId);
    }

    @Transactional
    public AdminSessionDetailsOutput rescheduleSession(UUID adminId, UUID sessionId, AdminRescheduleSessionInput input) {
        TrainingSession session = getManagedSession(adminId, sessionId);
        if (!canReschedule(session)) {
            throw new BadRequestException("Session cannot be rescheduled", sessionId, session.getStatus().name());
        }
        if (!input.startsAt().isBefore(input.endsAt())) {
            throw new BadRequestException("startsAt must be before endsAt", input.startsAt(), input.endsAt());
        }
        LocalDate newSessionDate = input.startsAt().toLocalDate();
        if (!newSessionDate.equals(input.endsAt().toLocalDate())) {
            throw new BadRequestException("Session must start and end on the same date", input.startsAt(), input.endsAt());
        }
        if (trainingSessionRepository.existsCoachConflict(session.getCoachId(), newSessionDate, input.startsAt(), input.endsAt(), sessionId)) {
            throw new BadRequestException("Coach has a conflicting session", session.getCoachId(), input.startsAt(), input.endsAt());
        }
        if (input.locationId() != null && trainingSessionRepository.existsLocationConflict(input.locationId(), newSessionDate, input.startsAt(), input.endsAt(), sessionId)) {
            throw new BadRequestException("Location has a conflicting session", input.locationId(), input.startsAt(), input.endsAt());
        }

        LocalDate previousSessionDate = session.getSessionDate();
        LocalDateTime previousStartsAt = session.getScheduledStartAt();
        LocalDateTime previousEndsAt = session.getScheduledEndAt();
        UUID previousLocationId = session.getLocationId();

        session.setSessionDate(newSessionDate);
        session.setScheduledStartAt(input.startsAt());
        session.setScheduledEndAt(input.endsAt());
        session.setLocationId(input.locationId());
        groupActivityPort.recordGroupActivity(
                session.getGroupId(),
                adminId,
                ACTIVITY_SESSION_RESCHEDULED,
                activityPayload()
                        .put("sessionId", session.getId())
                        .put("previousSessionDate", previousSessionDate)
                        .put("previousStartsAt", previousStartsAt)
                        .put("previousEndsAt", previousEndsAt)
                        .put("previousLocationId", previousLocationId)
                        .put("newSessionDate", session.getSessionDate())
                        .put("newStartsAt", session.getScheduledStartAt())
                        .put("newEndsAt", session.getScheduledEndAt())
                        .put("newLocationId", session.getLocationId())
                        .put("reason", normalizeComment(input.reason()))
                        .build()
        );
        return getSessionDetails(adminId, sessionId);
    }

    @Transactional
    public AdminSessionDetailsOutput substituteCoach(UUID adminId, UUID sessionId, AdminSubstituteCoachInput input) {
        TrainingSession session = getManagedSession(adminId, sessionId);
        if (!canSubstituteCoach(session)) {
            throw new BadRequestException("Session coach cannot be substituted", sessionId, session.getStatus().name());
        }
        if (!session.getCoachId().equals(input.replacedCoachId())) {
            throw new BadRequestException("replacedCoachId does not match session coach", input.replacedCoachId(), session.getCoachId());
        }
        if (input.replacedCoachId().equals(input.substituteCoachId())) {
            throw new BadRequestException("substituteCoachId must be different from replacedCoachId", input.substituteCoachId());
        }
        if (!coachPort.verifyCoach(input.substituteCoachId())) {
            throw new NotFoundException("Coach not found", input.substituteCoachId());
        }

        boolean substituteAssignedToGroup = groupCoachPort.getActiveCoaches(session.getGroupId()).stream()
                .anyMatch(item -> input.substituteCoachId().equals(item.coachId()));
        if (!substituteAssignedToGroup) {
            throw new BadRequestException("Substitute coach is not assigned to the group", input.substituteCoachId(), session.getGroupId());
        }
        if (trainingSessionRepository.existsCoachConflict(
                input.substituteCoachId(),
                session.getSessionDate(),
                session.getScheduledStartAt(),
                session.getScheduledEndAt(),
                sessionId
        )) {
            throw new BadRequestException("Substitute coach has a conflicting session", input.substituteCoachId(), session.getSessionDate());
        }

        UUID previousCoachId = session.getCoachId();
        session.setCoachId(input.substituteCoachId());
        groupActivityPort.recordGroupActivity(
                session.getGroupId(),
                adminId,
                ACTIVITY_SESSION_COACH_SUBSTITUTED,
                activityPayload()
                        .put("sessionId", session.getId())
                        .put("sessionDate", session.getSessionDate())
                        .put("startsAt", session.getScheduledStartAt())
                        .put("replacedCoachId", previousCoachId)
                        .put("substituteCoachId", input.substituteCoachId())
                        .put("reason", normalizeComment(input.reason()))
                        .build()
        );
        return getSessionDetails(adminId, sessionId);
    }

    @Transactional
    public AdminSessionAttendanceOutput updateSessionAttendance(
            UUID adminId,
            UUID sessionId,
            AdminSessionAttendanceUpdateInput input
    ) {
        TrainingSession session = getManagedSession(adminId, sessionId);
        if (!canEditAttendance(session)) {
            throw new BadRequestException("Attendance can be updated only for IN_PROGRESS or OVERDUE sessions", sessionId);
        }

        List<CoachRosterReader.ActivePlayerView> activePlayers =
                coachRosterReader.getActivePlayersByGroupAndDate(session.getGroupId(), session.getSessionDate());
        Set<UUID> activePlayerIds = activePlayers.stream()
                .map(CoachRosterReader.ActivePlayerView::id)
                .collect(Collectors.toSet());

        Set<UUID> duplicateCheck = new HashSet<>();
        for (AdminSessionAttendanceUpdateInput.Entry entry : input.entries()) {
            if (!duplicateCheck.add(entry.playerId())) {
                throw new BadRequestException("Duplicate player in attendance update", entry.playerId());
            }
            if (!activePlayerIds.contains(entry.playerId())) {
                throw new BadRequestException("Player does not belong to active group roster", entry.playerId());
            }
        }

        Map<UUID, TrainingSessionAttendance> existing = trainingSessionAttendanceRepository.findBySessionId(sessionId)
                .stream()
                .collect(Collectors.toMap(TrainingSessionAttendance::getPlayerId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        List<TrainingSessionAttendance> toSave = input.entries().stream()
                .map(entry -> {
                    TrainingSessionAttendance item = existing.get(entry.playerId());
                    if (item == null) {
                        item = TrainingSessionAttendance.builder()
                                .id(UUID.randomUUID())
                                .sessionId(sessionId)
                                .playerId(entry.playerId())
                                .build();
                    }
                    item.setStatus(entry.status());
                    item.setComment(normalizeComment(entry.comment()));
                    item.setMarkedBy(adminId);
                    item.setMarkedAt(now);
                    return item;
                })
                .toList();

        trainingSessionAttendanceRepository.saveAll(toSave);
        AdminSessionAttendanceOutput.Summary summary = toDetailedAttendanceSummary(
                trainingSessionAttendanceRepository.findBySessionId(sessionId),
                activePlayers.size()
        );
        groupActivityPort.recordGroupActivity(
                session.getGroupId(),
                adminId,
                ACTIVITY_ATTENDANCE_UPDATED,
                activityPayload()
                        .put("sessionId", session.getId())
                        .put("sessionDate", session.getSessionDate())
                        .put("startsAt", session.getScheduledStartAt())
                        .put("total", summary.total())
                        .put("marked", summary.marked())
                        .put("presentLike", summary.presentLike())
                        .build()
        );
        return getSessionAttendance(adminId, sessionId);
    }

    private ActivityPayloadBuilder activityPayload() {
        return new ActivityPayloadBuilder();
    }

    private static final class ActivityPayloadBuilder {
        private final java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();

        private ActivityPayloadBuilder put(String key, Object value) {
            values.put(key, value == null ? null : value.toString());
            return this;
        }

        private Map<String, Object> build() {
            return values;
        }
    }

    private AdminGroupSessionsOutput.SessionItem toSessionItem(
            TrainingSession session,
            Map<UUID, CoachDto> coachById,
            Map<UUID, GroupCoachDto> groupCoachByCoachId,
            Map<UUID, List<TrainingSessionAttendance>> attendanceBySessionId,
            Map<UUID, String> locationNames
    ) {
        int participantsCount = coachRosterReader.getActivePlayersByGroupAndDate(session.getGroupId(), session.getSessionDate()).size();
        AdminGroupSessionsOutput.AttendanceSummary attendance = toAttendanceSummary(
                attendanceBySessionId.getOrDefault(session.getId(), List.of()),
                participantsCount
        );

        return new AdminGroupSessionsOutput.SessionItem(
                session.getId(),
                session.getScheduleId(),
                session.getSessionDate(),
                session.getScheduledStartAt(),
                session.getScheduledEndAt(),
                session.getStatus().name(),
                resolveEffectiveStatus(session),
                session.getCancelReason(),
                toLocationRef(session.getLocationId(), locationNames),
                List.of(toCoachRef(session.getCoachId(), coachById, groupCoachByCoachId)),
                participantsCount,
                attendance,
                buildCapabilities(session)
        );
    }

    private AdminGroupSessionsOutput.CoachRef toCoachRef(
            UUID coachId,
            Map<UUID, CoachDto> coachById,
            Map<UUID, GroupCoachDto> groupCoachByCoachId
    ) {
        CoachDto coach = coachById.get(coachId);
        GroupCoachDto assignment = groupCoachByCoachId.get(coachId);
        String fullName = coach == null
                ? "Unknown coach"
                : StreamUtil.joinNonBlank(coach.firstName(), coach.lastName());
        String role = assignment == null || assignment.role() == null
                ? null
                : assignment.role().name();
        return new AdminGroupSessionsOutput.CoachRef(coachId, fullName, role);
    }

    private AdminGroupSessionsOutput.AttendanceSummary toAttendanceSummary(
            Collection<TrainingSessionAttendance> attendanceEntries,
            int participantsCount
    ) {
        int marked = attendanceEntries.size();
        int presentLike = (int) attendanceEntries.stream()
                .filter(item -> item.getStatus() == TrainingSessionAttendanceStatus.PRESENT
                        || item.getStatus() == TrainingSessionAttendanceStatus.LATE)
                .count();
        return new AdminGroupSessionsOutput.AttendanceSummary(participantsCount, marked, presentLike);
    }

    private AdminSessionAttendanceOutput.Summary toDetailedAttendanceSummary(
            Collection<TrainingSessionAttendance> attendanceEntries,
            int participantsCount
    ) {
        int marked = attendanceEntries.size();
        int present = 0;
        int absent = 0;
        int excused = 0;
        int late = 0;

        for (TrainingSessionAttendance entry : attendanceEntries) {
            if (entry.getStatus() == null) {
                continue;
            }
            switch (entry.getStatus()) {
                case PRESENT -> present++;
                case ABSENT -> absent++;
                case EXCUSED -> excused++;
                case LATE -> late++;
            }
        }

        int presentLike = present + late;
        int unmarked = Math.max(participantsCount - marked, 0);
        return new AdminSessionAttendanceOutput.Summary(
                participantsCount,
                marked,
                present,
                absent,
                excused,
                late,
                unmarked,
                presentLike
        );
    }

    private AdminGroupSessionsOutput.Capabilities buildCapabilities(TrainingSession session) {
        return new AdminGroupSessionsOutput.Capabilities(
                canCancel(session),
                canReschedule(session),
                canSubstituteCoach(session),
                canOpenAttendance(session)
        );
    }

    private String resolveEffectiveStatus(TrainingSession session) {
        return isOverdue(session) ? "OVERDUE" : session.getStatus().name();
    }

    private boolean isOverdue(TrainingSession session) {
        if (session.getStatus() != TrainingSessionStatus.PLANNED && session.getStatus() != TrainingSessionStatus.IN_PROGRESS) {
            return false;
        }
        return session.getScheduledEndAt() != null && session.getScheduledEndAt().isBefore(LocalDateTime.now());
    }

    private Map<UUID, CoachDto> getCoachMap(Set<UUID> coachIds) {
        if (coachIds.isEmpty()) {
            return Map.of();
        }
        return coachPort.getCoaches(coachIds).stream()
                .collect(Collectors.toMap(CoachDto::id, Function.identity()));
    }

    private Map<UUID, String> getLocationNames(Set<UUID> locationIds) {
        if (locationIds.isEmpty()) {
            return Map.of();
        }
        return locationRepository.findAllById(locationIds).stream()
                .collect(Collectors.toMap(Location::getId, Location::getName));
    }

    private MediaAssetResponse getGroupAvatar(UUID groupId) {
        java.util.Optional<MediaAsset> activeAvatar = mediaAvatarPort.findActiveAvatar(MediaOwnerType.GROUP, groupId);
        MediaAsset avatar = activeAvatar == null ? null : activeAvatar.orElse(null);
        return avatar == null ? null : mediaAccessPort.toResponse(avatar);
    }

    private AdminGroupSessionsOutput.LocationRef toLocationRef(UUID locationId, Map<UUID, String> locationNames) {
        if (locationId == null) {
            return null;
        }
        return new AdminGroupSessionsOutput.LocationRef(locationId, locationNames.get(locationId));
    }

    private boolean canCancel(TrainingSession session) {
        return !isOverdue(session)
                && (session.getStatus() == TrainingSessionStatus.PLANNED || session.getStatus() == TrainingSessionStatus.IN_PROGRESS);
    }

    private boolean canReschedule(TrainingSession session) {
        return !isOverdue(session) && session.getStatus() == TrainingSessionStatus.PLANNED;
    }

    private boolean canSubstituteCoach(TrainingSession session) {
        return !isOverdue(session)
                && (session.getStatus() == TrainingSessionStatus.PLANNED || session.getStatus() == TrainingSessionStatus.IN_PROGRESS);
    }

    private boolean canEditAttendance(TrainingSession session) {
        return session.getStatus() != TrainingSessionStatus.CANCELLED
                && (session.getStatus() == TrainingSessionStatus.IN_PROGRESS || isOverdue(session));
    }

    private boolean canOpenAttendance(TrainingSession session) {
        return session.getStatus() != TrainingSessionStatus.CANCELLED;
    }

    private String buildCancelReason(String reasonCode, String comment) {
        if (comment == null || comment.isBlank()) {
            return reasonCode;
        }
        return reasonCode + ": " + comment.trim();
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String normalized = comment.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private TrainingSession getManagedSession(UUID adminId, UUID sessionId) {
        verifyAdmin(adminId);
        TrainingSession session = trainingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found", sessionId));
        GroupDto group = groupPort.getGroupById(session.getGroupId());
        verifyAdminBranchAccess(adminId, group.branchId());
        return session;
    }

    private void verifyAdmin(UUID adminId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
    }

    private void verifyAdminBranchAccess(UUID adminId, UUID branchId) {
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("from and to are required", from, to);
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from cannot be after to", from, to);
        }
        long days = to.toEpochDay() - from.toEpochDay() + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BadRequestException("Sessions range cannot exceed 31 days", from, to);
        }
    }

    private static final class StreamUtil {
        private StreamUtil() {}

        private static String joinNonBlank(String first, String second) {
            return java.util.stream.Stream.of(first, second)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.joining(" "));
        }
    }
}
