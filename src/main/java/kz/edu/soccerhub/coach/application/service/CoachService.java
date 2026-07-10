package kz.edu.soccerhub.coach.application.service;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.auth.domain.repository.AppUserRepo;
import kz.edu.soccerhub.coach.domain.model.CoachBranch;
import kz.edu.soccerhub.coach.domain.model.CoachProfile;
import kz.edu.soccerhub.coach.domain.model.CoachStatusHistory;
import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.TrainingSessionAttendance;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.model.enums.WorkStatus;
import kz.edu.soccerhub.coach.domain.repository.CoachBranchRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachProfileRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachStatusHistoryRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionAttendanceRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import kz.edu.soccerhub.common.dto.coach.CoachCreateCommand;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRecordDto;
import kz.edu.soccerhub.common.dto.coach.CoachSessionAdminView;
import kz.edu.soccerhub.common.dto.coach.CoachStatusHistoryDto;
import kz.edu.soccerhub.common.dto.coach.CoachUpdateCommand;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRateDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceSummaryDto;
import kz.edu.soccerhub.common.dto.coach.SessionAttendanceSummaryDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachService implements CoachPort {

    private final CoachProfileRepository coachProfileRepository;
    private final CoachBranchService coachBranchService;
    private final CoachBranchRepository coachBranchRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingSessionAttendanceRepository trainingSessionAttendanceRepository;
    private final CoachStatusHistoryRepository coachStatusHistoryRepository;
    private final AppUserRepo appUserRepo;
    private final GroupPort groupPort;
    private final GroupScheduleRepository groupScheduleRepository;

    @Transactional
    public UUID create(CoachCreateCommand command) {
        CoachProfile profile = CoachProfile.builder()
                .id(command.id())
                .firstName(command.firstName())
                .lastName(command.lastName())
                .birthDate(command.birthDate())
                .phone(command.phone())
                .email(command.email())
                .accountStatus(AccountStatus.ACTIVE)
                .workStatus(WorkStatus.AVAILABLE)
                .build();

        coachProfileRepository.save(profile);
        return profile.getId();
    }

    @Override
    @Transactional
    public void update(CoachUpdateCommand command) {
        CoachProfile profile = coachProfileRepository.findById(command.coachId())
                .orElseThrow(() -> new NotFoundException("Coach not found", command.coachId()));

        profile.setFirstName(command.firstName().trim());
        profile.setLastName(command.lastName().trim());
        profile.setEmail(command.email().trim().toLowerCase());
        profile.setPhone(trimToNull(command.phone()));
        profile.setSpecialization(trimToNull(command.specialization()));

        appUserRepo.findById(command.coachId()).ifPresent(user -> user.setEmail(profile.getEmail()));
    }

    @Transactional
    public void assignToBranch(@NotNull UUID coachId, @NotNull UUID branchId) {
        Optional<CoachProfile> coachProfileOptional = coachProfileRepository.findById(coachId);
        if (coachProfileOptional.isEmpty()) {
            throw new NotFoundException("Coach not found", coachId);
        }
        if (coachProfileOptional.get().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new NotFoundException("Coach is not active", coachId);
        }
        coachBranchService.assignToBranch(coachId, branchId);
    }

    @Override
    public void unassignFromBranch(UUID coachId, UUID branchId) {
        boolean exists = coachProfileRepository.existsById(coachId);
        if (!exists) {
            throw new NotFoundException("Coach not found", coachId);
        }
        coachBranchService.unassignFromBranch(coachId, branchId);
    }

    @Transactional(readOnly = true)
    public CoachDto getCoach(UUID coachId) {
        return findById(coachId)
                .orElseThrow(() -> new NotFoundException("Coach not found", coachId));
    }

    @Override
    public Collection<CoachDto> getCoaches(Set<UUID> coachIds) {
        return coachProfileRepository.findAllById(coachIds)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean verifyCoach(UUID coachId) {
        return coachProfileRepository.existsById(coachId);
    }

    @Transactional(readOnly = true)
    public Optional<CoachDto> findById(UUID coachId) {
        return coachProfileRepository.findById(coachId)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CoachDto> getCoaches(Set<UUID> branchIds, Pageable pageable) {
        return coachProfileRepository
                .findAccessibleCoaches(branchIds, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public void enableCoach(UUID coachId) {
        coachProfileRepository.findById(coachId)
                .ifPresent(coachProfile -> coachProfile.setAccountStatus(AccountStatus.ACTIVE));
    }

    @Override
    public void disableCoach(UUID coachId) {
        coachProfileRepository.findById(coachId)
                .ifPresent(coachProfile -> coachProfile.setAccountStatus(AccountStatus.INACTIVE));
    }

    @Override
    @Transactional
    public void updateWorkStatus(UUID coachId, WorkStatus workStatus, LocalDate vacationFrom, LocalDate vacationTo, String reason) {
        CoachProfile profile = coachProfileRepository.findById(coachId)
                .orElseThrow(() -> new NotFoundException("Coach not found", coachId));
        profile.setWorkStatus(workStatus);
        profile.setVacationFrom(workStatus == WorkStatus.VACATION ? vacationFrom : null);
        profile.setVacationTo(workStatus == WorkStatus.VACATION ? vacationTo : null);
        profile.setWorkStatusReason(reason);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getBranchIds(UUID coachId) {
        return coachBranchRepository.findAllByCoachId(coachId).stream()
                .map(CoachBranch::getBranchId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getSessions(
            Set<UUID> coachIds,
            Set<UUID> groupIds,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (coachIds.isEmpty() || groupIds.isEmpty()) {
            return List.of();
        }
        List<TrainingSession> sessions = trainingSessionRepository
                .findByCoachIdInAndGroupIdInAndSessionDateBetween(coachIds, groupIds, dateFrom, dateTo)
                .stream()
                .toList();
        return toSessionAdminViews(sessions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getOverdueReportSessions(
            Set<UUID> coachIds,
            Set<UUID> groupIds,
            LocalDate beforeDate
    ) {
        if (coachIds.isEmpty() || groupIds.isEmpty()) {
            return List.of();
        }
        List<TrainingSession> sessions = trainingSessionRepository
                .findByCoachIdInAndGroupIdInAndSessionDateBeforeAndReportDoneFalse(coachIds, groupIds, beforeDate)
                .stream()
                .filter(session -> session.getStatus() != TrainingSessionStatus.CANCELLED)
                .toList();
        return toSessionAdminViews(sessions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getReportedSessions(Set<UUID> coachIds, Set<UUID> groupIds) {
        if (coachIds.isEmpty() || groupIds.isEmpty()) {
            return List.of();
        }
        return toSessionAdminViews(
                trainingSessionRepository.findByCoachIdInAndGroupIdInAndReportDoneTrue(coachIds, groupIds)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getUpcomingSessions(UUID coachId, LocalDate fromDate) {
        return toSessionAdminViews(trainingSessionRepository
                .findByCoachIdAndSessionDateGreaterThanEqualOrderBySessionDateAscScheduledStartAtAsc(coachId, fromDate)
                .stream()
                .filter(session -> session.getStatus() != TrainingSessionStatus.CANCELLED)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public int countOverdueReports(UUID coachId, LocalDate beforeDate) {
        return trainingSessionRepository.countByCoachIdAndSessionDateBeforeAndReportDoneFalseAndStatusNot(
                coachId,
                beforeDate,
                TrainingSessionStatus.CANCELLED
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getReportedSessions(UUID coachId) {
        return toSessionAdminViews(
                trainingSessionRepository.findByCoachIdAndReportDoneTrueOrderByUpdatedAtDesc(coachId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachStatusHistoryDto> getStatusHistory(UUID coachId) {
        return coachStatusHistoryRepository.findByCoachIdOrderByChangedAtDesc(coachId)
                .stream()
                .map(item -> new CoachStatusHistoryDto(
                        item.getStatus().name(),
                        item.getChangedAt(),
                        item.getChangedBy()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void recordStatusHistory(UUID coachId, CoachStatus status, UUID changedBy) {
        coachStatusHistoryRepository.save(CoachStatusHistory.builder()
                .id(UUID.randomUUID())
                .coachId(coachId)
                .status(status)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerAttendanceRateDto> getAttendanceRates(UUID groupId, Set<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }

        List<TrainingSession> sessions = trainingSessionRepository.findByGroupId(groupId);
        if (sessions.isEmpty()) {
            return playerIds.stream()
                    .map(playerId -> new PlayerAttendanceRateDto(playerId, 0))
                    .toList();
        }

        Set<UUID> sessionIds = sessions.stream()
                .map(TrainingSession::getId)
                .collect(Collectors.toSet());

        Map<UUID, List<TrainingSessionAttendance>> attendanceByPlayerId =
                trainingSessionAttendanceRepository.findBySessionIdIn(sessionIds).stream()
                        .filter(attendance -> playerIds.contains(attendance.getPlayerId()))
                        .collect(Collectors.groupingBy(TrainingSessionAttendance::getPlayerId));

        return playerIds.stream()
                .map(playerId -> new PlayerAttendanceRateDto(
                        playerId,
                        calculateAttendanceRate(attendanceByPlayerId.getOrDefault(playerId, List.of()))
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerAttendanceSummaryDto> getAttendanceSummaries(Set<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }

        List<TrainingSessionAttendance> attendances = trainingSessionAttendanceRepository.findByPlayerIdIn(playerIds);
        if (attendances.isEmpty()) {
            return playerIds.stream()
                    .map(playerId -> new PlayerAttendanceSummaryDto(playerId, 0, 0, 0, 0, 0, 0))
                    .toList();
        }

        Set<UUID> sessionIds = attendances.stream()
                .map(TrainingSessionAttendance::getSessionId)
                .collect(Collectors.toSet());
        Map<UUID, TrainingSession> sessionsById = trainingSessionRepository.findAllById(sessionIds).stream()
                .collect(Collectors.toMap(TrainingSession::getId, item -> item));
        LocalDate last30Boundary = LocalDate.now().minusDays(30);

        Map<UUID, List<TrainingSessionAttendance>> byPlayerId = attendances.stream()
                .collect(Collectors.groupingBy(TrainingSessionAttendance::getPlayerId));

        return playerIds.stream()
                .map(playerId -> summarizeAttendance(playerId, byPlayerId.getOrDefault(playerId, List.of()), sessionsById, last30Boundary))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerAttendanceRecordDto> getRecentAttendance(UUID playerId, int limit) {
        if (playerId == null || limit <= 0) {
            return List.of();
        }

        List<TrainingSessionAttendance> attendances = trainingSessionAttendanceRepository.findByPlayerId(playerId);
        if (attendances.isEmpty()) {
            return List.of();
        }

        Set<UUID> sessionIds = attendances.stream()
                .map(TrainingSessionAttendance::getSessionId)
                .collect(Collectors.toSet());
        Map<UUID, TrainingSession> sessionsById = trainingSessionRepository.findAllById(sessionIds).stream()
                .collect(Collectors.toMap(TrainingSession::getId, item -> item));
        Set<UUID> groupIds = sessionsById.values().stream()
                .map(TrainingSession::getGroupId)
                .collect(Collectors.toSet());
        Map<UUID, GroupDto> groupsById = groupIds.isEmpty()
                ? Map.of()
                : groupPort.getGroupsByIds(groupIds).stream().collect(Collectors.toMap(GroupDto::groupId, item -> item));

        return attendances.stream()
                .map(attendance -> toAttendanceRecord(attendance, sessionsById.get(attendance.getSessionId()), groupsById))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PlayerAttendanceRecordDto::sessionDate).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionAttendanceSummaryDto> getSessionAttendanceSummaries(Set<UUID> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<TrainingSessionAttendance>> attendanceBySessionId = trainingSessionAttendanceRepository.findBySessionIdIn(sessionIds)
                .stream()
                .collect(Collectors.groupingBy(TrainingSessionAttendance::getSessionId));

        return sessionIds.stream()
                .map(sessionId -> {
                    List<TrainingSessionAttendance> items = attendanceBySessionId.getOrDefault(sessionId, List.of());
                    long presentLikeMarked = items.stream()
                            .filter(attendance -> attendance.getStatus() == TrainingSessionAttendanceStatus.PRESENT
                                    || attendance.getStatus() == TrainingSessionAttendanceStatus.LATE)
                            .count();
                    return new SessionAttendanceSummaryDto(sessionId, items.size(), presentLikeMarked);
                })
                .toList();
    }

    private int calculateAttendanceRate(List<TrainingSessionAttendance> attendances) {
        if (attendances.isEmpty()) {
            return 0;
        }

        long attended = attendances.stream()
                .filter(attendance -> attendance.getStatus() == TrainingSessionAttendanceStatus.PRESENT
                        || attendance.getStatus() == TrainingSessionAttendanceStatus.LATE)
                .count();

        return (int) Math.round((attended * 100.0) / attendances.size());
    }

    private PlayerAttendanceSummaryDto summarizeAttendance(
            UUID playerId,
            List<TrainingSessionAttendance> attendances,
            Map<UUID, TrainingSession> sessionsById,
            LocalDate last30Boundary
    ) {
        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;
        int excusedCount = 0;
        int missedLast30Days = 0;

        for (TrainingSessionAttendance attendance : attendances) {
            switch (attendance.getStatus()) {
                case PRESENT -> presentCount++;
                case ABSENT -> absentCount++;
                case LATE -> lateCount++;
                case EXCUSED -> excusedCount++;
            }

            TrainingSession session = sessionsById.get(attendance.getSessionId());
            if (session != null
                    && !session.getSessionDate().isBefore(last30Boundary)
                    && attendance.getStatus() == TrainingSessionAttendanceStatus.ABSENT) {
                missedLast30Days++;
            }
        }

        return new PlayerAttendanceSummaryDto(
                playerId,
                calculateAttendanceRate(attendances),
                presentCount,
                absentCount,
                lateCount,
                excusedCount,
                missedLast30Days
        );
    }

    private PlayerAttendanceRecordDto toAttendanceRecord(
            TrainingSessionAttendance attendance,
            TrainingSession session,
            Map<UUID, GroupDto> groupsById
    ) {
        if (session == null) {
            return null;
        }
        GroupDto group = groupsById.get(session.getGroupId());
        return new PlayerAttendanceRecordDto(
                attendance.getPlayerId(),
                session.getId(),
                session.getSessionDate(),
                session.getGroupId(),
                group == null ? null : group.name(),
                attendance.getStatus()
        );
    }

    private CoachDto toDto(@NotNull CoachProfile coachProfile) {
        return CoachDto.builder()
                .id(coachProfile.getId())
                .firstName(coachProfile.getFirstName())
                .lastName(coachProfile.getLastName())
                .phone(coachProfile.getPhone())
                .email(coachProfile.getEmail())
                .specialization(coachProfile.getSpecialization())
                .active(coachProfile.getAccountStatus() == AccountStatus.ACTIVE)
                .accountStatus(coachProfile.getAccountStatus())
                .workStatus(coachProfile.getWorkStatus())
                .vacationFrom(coachProfile.getVacationFrom())
                .vacationTo(coachProfile.getVacationTo())
                .workStatusReason(coachProfile.getWorkStatusReason())
                .createdAt(coachProfile.getCreatedAt())
                .build();
    }

    private List<CoachSessionAdminView> toSessionAdminViews(List<TrainingSession> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> scheduleTypesById = groupScheduleRepository.findAllById(
                        sessions.stream()
                                .map(TrainingSession::getScheduleId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(GroupSchedule::getId, schedule -> schedule.getScheduleType().name()));
        return sessions.stream()
                .map(session -> toSessionAdminView(session, scheduleTypesById.get(session.getScheduleId())))
                .toList();
    }

    private CoachSessionAdminView toSessionAdminView(TrainingSession session, String scheduleType) {
        return new CoachSessionAdminView(
                session.getId(),
                session.getCoachId(),
                session.getGroupId(),
                session.getScheduleId(),
                scheduleType,
                session.getSessionDate(),
                session.getScheduledStartAt(),
                session.getScheduledEndAt(),
                session.getStatus().name(),
                session.isReportDone(),
                session.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
