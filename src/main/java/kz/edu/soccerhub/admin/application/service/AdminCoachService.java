package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.coach.*;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import kz.edu.soccerhub.common.dto.coach.*;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.dto.lead.AvailableSlotOutput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.dispatcher.application.service.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCoachService {

    private final CoachPort coachPort;
    private final AuthPort authPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final PasswordGenerator passwordGenerator;
    private final GroupSchedulePort groupSchedulePort;
    private final GroupPort groupPort;
    private final GroupCoachPort groupCoachPort;
    private final ClientPort clientPort;

    @Transactional
    public AdminCreateCoachOutput createCoach(UUID adminId, AdminCreateCoachInput input) {
        final String tempPassword = passwordGenerator.generate(6);

        AuthRegisterCommand authRegisterCommand = AuthRegisterCommand.builder()
                .email(input.email())
                .password(tempPassword)
                .roles(Set.of(Role.COACH))
                .requireToChangePassword(true)
                .build();

        AuthRegisterCommandOutput output = authPort.register(authRegisterCommand);

        CoachCreateCommand command = CoachCreateCommand.builder()
                .id(output.id())
                .firstName(input.firstName())
                .lastName(input.lastName())
                .email(input.email())
                .phone(input.phone())
                .build();

        UUID coachId = coachPort.create(command);
        coachPort.recordStatusHistory(coachId, kz.edu.soccerhub.coach.domain.model.enums.CoachStatus.ACTIVE, adminId);
        log.info("Admin [{}] creates coach: {}", adminId, coachId);

        UUID branchId = input.branchId();
        if (branchId != null) {
            assignCoachToBranch(adminId, coachId, branchId);
            log.info("Admin {} assigned coach {} to the branch {}", adminId, coachId, branchId);
        }

        return AdminCreateCoachOutput.builder()
                .coachId(coachId)
                .tempPassword(tempPassword)
                .build();
    }

    @Transactional
    public void assignCoachToBranch(UUID adminId, UUID coachId, UUID branchId) {
        boolean isAdminHasAccessToBranch = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!isAdminHasAccessToBranch) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }

        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        coachPort.assignToBranch(coachId, branchId);
    }

    @Transactional
    public void unassignCoachFromBranch(UUID adminId, UUID coachId, UUID branchId) {
        boolean isAdminHasAccessToBranch = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!isAdminHasAccessToBranch) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }

        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        coachPort.unassignFromBranch(coachId, branchId);
    }

    @Transactional(readOnly = true)
    public Page<CoachDto> getCoachByBranchId(UUID adminId, UUID branchId, Pageable pageable) {
        boolean isAdminHasAccessToBranch = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!isAdminHasAccessToBranch) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
        return coachPort.getCoaches(Set.of(branchId), pageable);
    }

    @Transactional
    public void updateCoachStatus(UUID adminId, UUID coachId, AdminCoachUpdateCoachStatusInput input) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        switch (input.status()) {
            case ACTIVE -> coachPort.enableCoach(coachId);
            case INACTIVE -> coachPort.disableCoach(coachId);
            default -> throw new BadRequestException("Invalid coach status", input.status());
        }

        coachPort.recordStatusHistory(coachId, input.status(), adminId);
    }

    @Transactional
    public void updateCoach(UUID adminId, UUID coachId, AdminCoachUpdateInput input) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        ensureAdminHasCoachAccess(adminId, coachId);

        coachPort.update(CoachUpdateCommand.builder()
                .coachId(coachId)
                .firstName(input.firstName())
                .lastName(input.lastName())
                .email(input.email())
                .phone(input.phone())
                .specialization(input.specialization())
                .build());
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotOutput> getCoachAvailableSlots(UUID adminId, UUID coachId, LocalDate date) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        if (!coachPort.verifyCoach(coachId)) {
            throw new NotFoundException("Coach not found", coachId);
        }

        return groupSchedulePort.getActiveSchedulesByCoach(coachId, date).stream()
                .map(slot -> new AvailableSlotOutput(date, slot.startTime(), slot.endTime()))
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCoachOverviewOutput getCoachesOverview(UUID adminId, UUID branchId) {
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
        return buildOverview(branchId);
    }

    @Transactional(readOnly = true)
    public AdminCoachProfileOutput getCoachProfile(UUID adminId, UUID coachId) {
        Set<UUID> allowedBranchIds = adminBranchService.getAdminBranches(adminId).stream()
                .map(kz.edu.soccerhub.admin.application.dto.branch.AdminBranchesOutput::branchId)
                .collect(Collectors.toSet());
        return buildProfile(coachId, allowedBranchIds);
    }

    private void ensureAdminHasCoachAccess(UUID adminId, UUID coachId) {
        Set<UUID> allowedBranchIds = adminBranchService.getAdminBranches(adminId).stream()
                .map(kz.edu.soccerhub.admin.application.dto.branch.AdminBranchesOutput::branchId)
                .collect(Collectors.toSet());
        Set<UUID> coachBranchIds = coachPort.getBranchIds(coachId);
        boolean hasAccess = coachBranchIds.stream().anyMatch(allowedBranchIds::contains);
        if (!hasAccess) {
            throw new BadRequestException("Admin does not have access to coach branches", coachId);
        }
    }

    private AdminCoachOverviewOutput buildOverview(UUID branchId) {
        List<CoachDto> coaches = coachPort.getCoaches(Set.of(branchId), Pageable.unpaged()).getContent();
        Set<UUID> coachIds = coaches.stream().map(CoachDto::id).collect(Collectors.toSet());
        if (coachIds.isEmpty()) {
            return new AdminCoachOverviewOutput(
                    new AdminCoachOverviewOutput.Summary(0, 0, 0, 0, 0, 0),
                    List.of()
            );
        }

        List<GroupDto> groups = new ArrayList<>(groupPort.getGroupsByBranch(branchId));
        Set<UUID> groupIds = groups.stream().map(GroupDto::groupId).collect(Collectors.toSet());
        Map<UUID, GroupDto> groupsById = groups.stream().collect(Collectors.toMap(GroupDto::groupId, group -> group));

        List<GroupCoachDto> groupCoaches = new ArrayList<>(
                groupCoachPort.getActiveAssignmentsByCoachIdsAndGroupIds(coachIds, groupIds)
        );
        Map<UUID, List<GroupCoachDto>> groupCoachesByCoachId = groupCoaches.stream()
                .collect(Collectors.groupingBy(GroupCoachDto::coachId));

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<CoachSessionAdminView> weekSessions = coachPort.getSessions(coachIds, groupIds, weekStart, weekEnd);
        List<CoachSessionAdminView> overdueSessions = coachPort.getOverdueReportSessions(coachIds, groupIds, today);
        List<CoachSessionAdminView> reportedSessions = coachPort.getReportedSessions(coachIds, groupIds);

        Map<UUID, Integer> weeklyByCoach = new HashMap<>();
        Map<UUID, Integer> todayByCoach = new HashMap<>();
        for (CoachSessionAdminView session : weekSessions) {
            if ("CANCELLED".equals(session.status())) {
                continue;
            }
            weeklyByCoach.merge(session.coachId(), 1, Integer::sum);
            if (today.equals(session.sessionDate())) {
                todayByCoach.merge(session.coachId(), 1, Integer::sum);
            }
        }

        Map<UUID, Integer> overdueByCoach = new HashMap<>();
        for (CoachSessionAdminView session : overdueSessions) {
            overdueByCoach.merge(session.coachId(), 1, Integer::sum);
        }

        Map<UUID, OffsetDateTime> lastReportByCoach = new HashMap<>();
        for (CoachSessionAdminView session : reportedSessions) {
            if (session.updatedAt() == null) {
                continue;
            }
            OffsetDateTime candidate = session.updatedAt().atOffset(ZoneOffset.UTC);
            lastReportByCoach.merge(
                    session.coachId(),
                    candidate,
                    (oldValue, newValue) -> newValue.isAfter(oldValue) ? newValue : oldValue
            );
        }

        int maxSlots = 12;
        List<AdminCoachOverviewOutput.CoachItem> items = new ArrayList<>();
        int active = 0;
        int withoutGroups = 0;
        int overloaded = 0;
        int withSessionsToday = 0;

        for (CoachDto coach : coaches) {
            if (coach.active()) {
                active++;
            }

            Set<UUID> seenGroupIds = new HashSet<>();
            List<AdminCoachOverviewOutput.GroupItem> coachGroups = groupCoachesByCoachId
                    .getOrDefault(coach.id(), List.of())
                    .stream()
                    .filter(link -> seenGroupIds.add(link.groupId()))
                    .map(link -> groupsById.get(link.groupId()))
                    .filter(java.util.Objects::nonNull)
                    .map(group -> new AdminCoachOverviewOutput.GroupItem(group.groupId(), group.name()))
                    .toList();

            if (coachGroups.isEmpty()) {
                withoutGroups++;
            }

            int weeklyCount = weeklyByCoach.getOrDefault(coach.id(), 0);
            int todayCount = todayByCoach.getOrDefault(coach.id(), 0);
            if (todayCount > 0) {
                withSessionsToday++;
            }

            String loadStatus = weeklyCount > maxSlots ? "OVERLOADED" : "NORMAL";
            if ("OVERLOADED".equals(loadStatus)) {
                overloaded++;
            }

            items.add(new AdminCoachOverviewOutput.CoachItem(
                    coach.id(),
                    coach.firstName(),
                    coach.lastName(),
                    coach.email(),
                    coach.phone(),
                    coach.active(),
                    coachGroups,
                    weeklyCount,
                    todayCount,
                    new AdminCoachOverviewOutput.Load(weeklyCount, maxSlots, loadStatus),
                    new AdminCoachOverviewOutput.Reports(
                            overdueByCoach.getOrDefault(coach.id(), 0),
                            lastReportByCoach.get(coach.id())
                    )
            ));
        }

        return new AdminCoachOverviewOutput(
                new AdminCoachOverviewOutput.Summary(
                        coaches.size(),
                        active,
                        coaches.size() - active,
                        withoutGroups,
                        overloaded,
                        withSessionsToday
                ),
                items
        );
    }

    private AdminCoachProfileOutput buildProfile(UUID coachId, Set<UUID> allowedBranchIds) {
        CoachDto coach = coachPort.getCoach(coachId);
        Set<UUID> coachBranchIds = coachPort.getBranchIds(coachId);
        boolean hasAccess = coachBranchIds.stream().anyMatch(allowedBranchIds::contains);
        if (!hasAccess) {
            throw new BadRequestException("Admin does not have access to coach branches", coachId);
        }

        List<GroupCoachDto> groupLinks = new ArrayList<>(groupCoachPort.getActiveAssignmentsByCoachId(coachId));
        Set<UUID> groupIds = groupLinks.stream().map(GroupCoachDto::groupId).collect(Collectors.toSet());
        Map<UUID, GroupDto> groupsById = groupPort.getGroupsByIds(groupIds).stream()
                .collect(Collectors.toMap(GroupDto::groupId, group -> group));

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<GroupScheduleDto> allActiveSchedules = groupSchedulePort.getActiveSchedulesByCoach(coachId);
        Map<UUID, Integer> weeklySlotsCountByGroupId = new HashMap<>();
        for (GroupScheduleDto schedule : allActiveSchedules) {
            weeklySlotsCountByGroupId.merge(schedule.groupId(), 1, Integer::sum);
        }

        List<GroupScheduleDto> schedules = allActiveSchedules.stream()
                .filter(schedule -> !schedule.endDate().isBefore(weekStart) && !schedule.startDate().isAfter(weekEnd))
                .toList();
        List<AdminCoachProfileOutput.WeeklyScheduleItem> weeklySchedule = schedules.stream()
                .map(schedule -> {
                    GroupDto group = groupsById.get(schedule.groupId());
                    String groupName = group == null ? "Unknown group" : group.name();
                    return new AdminCoachProfileOutput.WeeklyScheduleItem(
                            schedule.scheduleId(),
                            schedule.dayOfWeek(),
                            schedule.startTime(),
                            schedule.endTime(),
                            schedule.groupId(),
                            groupName,
                            schedule.startDate(),
                            schedule.endDate()
                    );
                })
                .toList();

        int maxSlots = 12;
        long usedSlotsCount = coachPort.getSessions(Set.of(coachId), groupIds, weekStart, weekEnd).stream()
                .filter(session -> !"CANCELLED".equals(session.status()))
                .count();
        int usedSlots = Math.toIntExact(usedSlotsCount);
        String loadStatus = usedSlots > maxSlots ? "OVERLOADED" : "NORMAL";

        List<CoachSessionAdminView> upcomingSessionViews = coachPort.getUpcomingSessions(coachId, today);
        Map<UUID, CoachSessionAdminView> nextSessionByGroupId = upcomingSessionViews.stream()
                .sorted(Comparator.comparing(CoachSessionAdminView::sessionDate)
                        .thenComparing(CoachSessionAdminView::scheduledStartAt))
                .collect(Collectors.toMap(
                        CoachSessionAdminView::groupId,
                        session -> session,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        List<AdminCoachProfileOutput.UpcomingSessionItem> upcomingSessions = upcomingSessionViews.stream()
                .limit(10)
                .map(session -> {
                    GroupDto group = groupsById.get(session.groupId());
                    String groupName = group == null ? "Unknown group" : group.name();
                    return new AdminCoachProfileOutput.UpcomingSessionItem(
                            session.sessionId(),
                            session.sessionDate(),
                            session.scheduledStartAt().toLocalTime(),
                            session.scheduledEndAt().toLocalTime(),
                            session.groupId(),
                            groupName,
                            session.status(),
                            session.reportDone()
                    );
                })
                .toList();

        Map<UUID, List<GroupMemberDto>> membersByGroupId = groupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        clientPort::getGroupMembers
                ));

        Map<UUID, Boolean> overdueReportsByGroupId = coachPort.getOverdueReportSessions(Set.of(coachId), groupIds, today)
                .stream()
                .collect(Collectors.toMap(
                        CoachSessionAdminView::groupId,
                        session -> true,
                        (first, second) -> first
                ));

        List<CoachSessionAdminView> reported = coachPort.getReportedSessions(coachId);
        OffsetDateTime lastReportAt = reported.stream()
                .map(CoachSessionAdminView::updatedAt)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .map(updatedAt -> updatedAt.atOffset(ZoneOffset.UTC))
                .orElse(null);

        List<AdminCoachProfileOutput.RecentReportItem> recentReports = reported.stream()
                .filter(session -> session.updatedAt() != null)
                .limit(10)
                .map(session -> {
                    GroupDto group = groupsById.get(session.groupId());
                    String groupName = group == null ? "Unknown group" : group.name();
                    return new AdminCoachProfileOutput.RecentReportItem(
                            session.sessionId(),
                            session.sessionDate(),
                            session.scheduledStartAt().toLocalTime(),
                            session.groupId(),
                            groupName,
                            session.updatedAt().atOffset(ZoneOffset.UTC)
                    );
                })
                .toList();

        List<AdminCoachProfileOutput.StatusHistoryItem> statusHistory = coachPort.getStatusHistory(coachId).stream()
                .map(item -> new AdminCoachProfileOutput.StatusHistoryItem(
                        item.status(),
                        item.changedAt(),
                        item.changedBy()
                ))
                .toList();

        List<AdminCoachProfileOutput.GroupItem> groups = groupLinks.stream()
                .map(link -> {
                    GroupDto group = groupsById.get(link.groupId());
                    if (group == null) {
                        return null;
                    }

                    List<GroupMemberDto> members = membersByGroupId.getOrDefault(group.groupId(), List.of());
                    int studentsCount = members.size();
                    int activeStudentsCount = (int) members.stream()
                            .filter(this::isActiveGroupMember)
                            .count();
                    int weeklySlotsCount = weeklySlotsCountByGroupId.getOrDefault(group.groupId(), 0);
                    AdminCoachProfileOutput.NextSessionItem nextSession =
                            toNextSessionItem(nextSessionByGroupId.get(group.groupId()));
                    List<AdminCoachProfileOutput.RiskFlagItem> riskFlags = buildGroupRiskFlags(
                            activeStudentsCount,
                            weeklySlotsCount,
                            nextSession,
                            overdueReportsByGroupId.getOrDefault(group.groupId(), false)
                    );

                    return new AdminCoachProfileOutput.GroupItem(
                            group.groupId(),
                            group.name(),
                            group.branchId(),
                            link.id(),
                            link.role() == null ? null : link.role().name(),
                            studentsCount,
                            activeStudentsCount,
                            weeklySlotsCount,
                            nextSession,
                            riskFlags
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return new AdminCoachProfileOutput(
                coach.id(),
                coach.firstName(),
                coach.lastName(),
                coach.email(),
                coach.phone(),
                coach.specialization(),
                coach.active(),
                new AdminCoachProfileOutput.Load(usedSlots, maxSlots, loadStatus),
                groups,
                weeklySchedule,
                upcomingSessions,
                new AdminCoachProfileOutput.Reports(
                        coachPort.countOverdueReports(coachId, today),
                        lastReportAt,
                        recentReports
                ),
                statusHistory
        );
    }

    private AdminCoachProfileOutput.NextSessionItem toNextSessionItem(CoachSessionAdminView session) {
        if (session == null) {
            return null;
        }
        return new AdminCoachProfileOutput.NextSessionItem(
                session.sessionId(),
                session.sessionDate(),
                session.scheduledStartAt().toLocalTime(),
                session.scheduledEndAt().toLocalTime(),
                session.status(),
                session.reportDone()
        );
    }

    private List<AdminCoachProfileOutput.RiskFlagItem> buildGroupRiskFlags(
            int activeStudentsCount,
            int weeklySlotsCount,
            AdminCoachProfileOutput.NextSessionItem nextSession,
            boolean hasOverdueReports
    ) {
        List<AdminCoachProfileOutput.RiskFlagItem> riskFlags = new ArrayList<>();
        if (activeStudentsCount == 0) {
            riskFlags.add(new AdminCoachProfileOutput.RiskFlagItem(
                    "NO_STUDENTS",
                    "Нет активных учеников",
                    "WARNING"
            ));
        }
        if (weeklySlotsCount == 0) {
            riskFlags.add(new AdminCoachProfileOutput.RiskFlagItem(
                    "NO_SCHEDULE",
                    "Нет активных слотов в расписании",
                    "CRITICAL"
            ));
        }
        if (nextSession == null) {
            riskFlags.add(new AdminCoachProfileOutput.RiskFlagItem(
                    "NO_UPCOMING_SESSIONS",
                    "Нет ближайших тренировок",
                    "WARNING"
            ));
        }
        if (hasOverdueReports) {
            riskFlags.add(new AdminCoachProfileOutput.RiskFlagItem(
                    "OVERDUE_REPORTS",
                    "Есть просроченные отчеты",
                    "CRITICAL"
            ));
        }
        return List.copyOf(riskFlags);
    }

    private boolean isActiveGroupMember(GroupMemberDto member) {
        return member.contractStatus() == null || "ACTIVE".equalsIgnoreCase(member.contractStatus());
    }

    public AdminCoachResetPasswordOutput resetCoachPassword(UUID adminId, UUID coachId) {
        String tempPassword = passwordGenerator.generate(8);
        authPort.resetPassword(coachId, tempPassword);

        log.info("Coach password reset: coachId={}, adminId={}", coachId, adminId);
        return new AdminCoachResetPasswordOutput(tempPassword);
    }
}
