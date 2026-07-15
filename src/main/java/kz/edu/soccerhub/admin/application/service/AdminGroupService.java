package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.group.*;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRateDto;
import kz.edu.soccerhub.common.dto.group.*;
import kz.edu.soccerhub.common.dto.lead.AvailableSlotOutput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.organization.application.dto.CoachBusySlotView;
import kz.edu.soccerhub.organization.application.service.GroupScheduleValidationService;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGroupService {

    private final GroupPort groupPort;
    private final CoachPort coachPort;
    private final GroupCoachPort groupCoachPort;
    private final GroupSchedulePort groupSchedulePort;
    private final CoachAvailabilityPort coachAvailabilityPort;
    private final ClientPort clientPort;
    private final GroupScheduleValidationService groupScheduleValidationService;

    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final BranchPort branchPort;

    /* ================= GROUP ================= */

    @Transactional
    public UUID createGroup(UUID adminId, AdminGroupCreateInput input) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, input.branchId());

        CreateGroupCommand command = CreateGroupCommand.builder()
                .name(input.name())
                .description(input.description())
                .ageFrom(input.ageFrom())
                .ageTo(input.ageTo())
                .audienceType(input.audienceType())
                .capacity(input.capacity())
                .level(input.level())
                .branchId(input.branchId())
                .build();

        UUID groupId = groupPort.createGroup(command);

        log.info(
                "Admin {} created group {} in branch {}",
                adminId, groupId, input.branchId()
        );

        return groupId;
    }

    @Transactional(readOnly = true)
    public AdminGroupOverviewOutput getGroupsOverview(UUID adminId, UUID branchId) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, branchId);

        List<GroupDto> groups = new ArrayList<>(groupPort.getGroupsByBranch(branchId));
        List<GroupView> views = groups.stream()
                .map(this::buildGroupView)
                .toList();

        return new AdminGroupOverviewOutput(
                new AdminGroupOverviewOutput.Summary(
                        views.size(),
                        countByStatus(views, GroupStatus.ACTIVE),
                        countByStatus(views, GroupStatus.PAUSED),
                        countByStatus(views, GroupStatus.STOPPED),
                        (int) views.stream().filter(view -> view.coachesCount() == 0).count(),
                        (int) views.stream().filter(view -> !view.scheduleActive()).count(),
                        (int) views.stream().filter(GroupView::overCapacity).count()
                ),
                views.stream()
                        .map(GroupView::toOverviewItem)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public AdminGroupHealthOutput getGroupHealth(UUID adminId, UUID groupId) {
        verifyAdmin(adminId);
        GroupDto group = groupPort.getGroupById(groupId);
        verifyAdminBranchAccess(adminId, group.branchId());

        GroupView view = buildGroupView(group);

        return new AdminGroupHealthOutput(
                groupId,
                view.health(),
                view.issues(),
                buildRecommendedActions(view)
        );
    }

    @Transactional(readOnly = true)
    public AdminGroupDetailsOutput getGroupDetails(UUID adminId, UUID groupId) {
        verifyAdmin(adminId);
        GroupDto group = groupPort.getGroupById(groupId);
        verifyAdminBranchAccess(adminId, group.branchId());

        GroupView view = buildGroupView(group);
        BranchDto branch = branchPort.findById(group.branchId())
                .orElseThrow(() -> new NotFoundException("Branch not found", group.branchId()));

        return new AdminGroupDetailsOutput(
                group.groupId(),
                group.name(),
                group.description(),
                group.status(),
                group.ageFrom(),
                group.ageTo(),
                group.audienceType(),
                group.level(),
                group.capacity(),
                new AdminGroupDetailsOutput.BranchRef(branch.id(), branch.name()),
                new AdminGroupDetailsOutput.Summary(
                        view.studentsCount(),
                        view.coachesCount(),
                        view.sessionsPerWeek(),
                        view.occupancyPercent()
                ),
                view.health(),
                view.issues(),
                view.nextSessionAt() == null ? null : new AdminGroupDetailsOutput.NextSession(view.nextSessionAt()),
                buildCapabilities(group.status())
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupMemberOutput> getGroupMembers(UUID adminId, UUID groupId, Pageable pageable) {
        verifyAdmin(adminId);
        GroupDto group = groupPort.getGroupById(groupId);
        verifyAdminBranchAccess(adminId, group.branchId());

        List<GroupMemberDto> groupMembers = clientPort.getGroupMembers(groupId);
        if (groupMembers.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<UUID> playerIds = groupMembers.stream()
                .map(GroupMemberDto::playerId)
                .collect(Collectors.toSet());
        Map<UUID, Integer> attendanceRateByPlayer = coachPort.getAttendanceRates(groupId, playerIds).stream()
                .collect(Collectors.toMap(PlayerAttendanceRateDto::playerId, PlayerAttendanceRateDto::attendanceRate));

        List<AdminGroupMemberOutput> members = groupMembers.stream()
                .map(member -> new AdminGroupMemberOutput(
                        member.membershipId(),
                        member.clientId(),
                        member.playerId(),
                        member.childName(),
                        member.birthDate(),
                        attendanceRateByPlayer.getOrDefault(member.playerId(), 0),
                        member.contractStatus(),
                        member.contractStatus(),
                        member.joinedAt(),
                        member.leftAt(),
                        buildMemberCapabilities(member)
                ))
                .toList();

        int start = (int) pageable.getOffset();
        if (start >= members.size()) {
            return new PageImpl<>(List.of(), pageable, members.size());
        }

        int end = Math.min(start + pageable.getPageSize(), members.size());
        return new PageImpl<>(members.subList(start, end), pageable, members.size());
    }

    @Transactional(readOnly = true)
    public AdminGroupScheduleRiskOutput getScheduleRisks(UUID adminId, UUID groupId) {
        verifyAdmin(adminId);
        GroupDto group = groupPort.getGroupById(groupId);
        verifyAdminBranchAccess(adminId, group.branchId());

        List<GroupScheduleDto> schedules = groupSchedulePort.getActiveSchedulesByGroup(groupId);

        int conflictsCount = countScheduleConflicts(groupId, schedules);
        int emptyDaysCount = Math.max(0, DayOfWeek.values().length - distinctScheduleDays(schedules));

        return new AdminGroupScheduleRiskOutput(
                conflictsCount > 0,
                conflictsCount,
                emptyDaysCount,
                toOffsetDateTime(calculateNextSession(schedules))
        );
    }

    @Transactional
    public void updateGroupStatus(UUID adminId, UUID groupId, GroupStatus status) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());

        groupPort.updateStatus(groupId, status);

        log.info(
                "Admin {} updated group status {} to {}",
                adminId, groupId, status
        );
    }

    @Transactional
    public void updateGroup(UUID adminId, UUID groupId, AdminGroupUpdateInput input) {
        verifyAdmin(adminId);

        GroupDto currentGroup = groupPort.getGroupById(groupId);
        verifyAdminBranchAccess(adminId, currentGroup.branchId());

        UUID targetBranchId = input.branchId() != null ? input.branchId() : currentGroup.branchId();
        verifyAdminBranchAccess(adminId, targetBranchId);

        String targetName = input.name() != null ? input.name() : currentGroup.name();
        if (targetName == null || targetName.isBlank()) {
            throw new BadRequestException("Group name must not be blank");
        }

        GroupAudienceType targetAudienceType = input.audienceType() != null ? input.audienceType() : currentGroup.audienceType();
        Integer targetAgeFrom = input.ageFrom() != null ? input.ageFrom() : currentGroup.ageFrom();
        Integer targetAgeTo = input.ageTo() != null ? input.ageTo() : currentGroup.ageTo();
        Integer targetCapacity = input.capacity() != null ? input.capacity() : currentGroup.capacity();
        int studentsCount = countStudents(groupId);

        if (targetCapacity != null && targetCapacity < studentsCount) {
            throw new BadRequestException("Group capacity cannot be below current members count", targetCapacity, studentsCount);
        }
        if (targetAgeFrom != null && targetAgeTo != null && targetAgeFrom > targetAgeTo) {
            throw new BadRequestException("Invalid age range: 'from' age cannot be greater then 'to' age.", targetAgeFrom, targetAgeTo);
        }
        if (targetAudienceType == null) {
            throw new BadRequestException("Group audienceType must not be null");
        }
        if (input.level() == null && currentGroup.level() == null) {
            throw new BadRequestException("Group level must not be null");
        }

        groupPort.updateGroup(groupId, UpdateGroupCommand.builder()
                .name(targetName)
                .description(input.description() != null ? input.description() : currentGroup.description())
                .branchId(targetBranchId)
                .ageFrom(targetAgeFrom)
                .ageTo(targetAgeTo)
                .audienceType(targetAudienceType)
                .capacity(targetCapacity)
                .level(input.level() != null ? input.level() : currentGroup.level())
                .build());

        log.info("Admin {} updated group {}", adminId, groupId);
    }

    /* ================= GROUP COACH ================= */

    @Transactional
    public UUID assignCoachToGroup(UUID adminId, UUID groupId, UUID coachId, CoachRole role) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());
        verifyCoach(coachId);

        UUID assignmentId = groupCoachPort.assignCoach(groupId, coachId, role);

        log.info(
                "Admin {} assigned coach {} to group {}",
                adminId, coachId, groupId
        );

        return assignmentId;
    }

    @Transactional
    public void unassignCoachFromGroup(UUID adminId, UUID groupCoachId) {

        verifyAdmin(adminId);

        groupCoachPort.unassignCoach(groupCoachId);

        log.info(
                "Admin {} unassigned coach from group {}",
                adminId, groupCoachId
        );
    }

    @Transactional(readOnly = true)
    public Collection<AdminGroupCoachOutput> getGroupCoaches(UUID groupId) {
        log.debug("======= Fetching group -> {} coaches =======", groupId);

        Collection<GroupCoachDto> activeGroupCoaches =
                groupCoachPort.getActiveCoaches(groupId);

        log.debug("======= Group {} coaches ids {} =======", groupId, activeGroupCoaches);

        Collection<CoachDto> coaches = coachPort.getCoaches(
                activeGroupCoaches.stream()
                        .map(GroupCoachDto::coachId)
                        .collect(Collectors.toSet())
        );

        log.debug("======= Merge group coach and coach info =======");

        Map<UUID, CoachDto> coachMap = coaches.stream()
                .collect(Collectors.toMap(CoachDto::id, Function.identity()));

        return activeGroupCoaches.stream()
                .map(groupCoach -> {
                    CoachDto coach = coachMap.get(groupCoach.coachId());

                    if (coach == null) {
                        log.warn("Coach not found for coachId={}", groupCoach.coachId());
                        return null;
                    }

                    return AdminGroupCoachOutput.builder()
                            .groupCoachId(groupCoach.id())
                            .coachId(coach.id())
                            .groupId(groupCoach.groupId())
                            .coachFirstName(coach.firstName())
                            .coachLastName(coach.lastName())
                            .birthDate(coach.birthDate())
                            .phone(coach.phone())
                            .email(coach.email())
                            .active(coach.active())
                            .coachRole(groupCoach.role())
                            .assignedFrom(groupCoach.assignedFrom())
                            .assignedTo(groupCoach.assignedTo())
                            .createdAt(groupCoach.createdAt())
                            .updatedAt(groupCoach.updateAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /* ================= SCHEDULE ================= */

    @Transactional
    public void createGroupSchedule(
            UUID adminId,
            UUID groupId,
            GroupScheduleBatchCommand command
    ) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());

        groupSchedulePort.createSchedule(groupId, command);

        log.info(
                "Admin {} created schedule for group {}",
                adminId, groupId
        );
    }

    @Transactional
    public void cancelSchedule(UUID adminId, UUID scheduleId) {
        verifyAdmin(adminId);

        groupSchedulePort.cancelSchedule(scheduleId);

        log.info(
                "Admin {} cancelled schedule {}",
                adminId, scheduleId
        );
    }

    @Transactional
    public void activateSchedule(UUID adminId, UUID scheduleId) {
        verifyAdmin(adminId);

        groupSchedulePort.activateSchedule(scheduleId);

        log.info(
                "Admin {} activated schedule {}",
                adminId, scheduleId
        );
    }

    @Transactional
    public void cancelScheduleBatch(UUID adminId, UUID groupId, AdminCancelScheduleBatchInput input) {
        verifyAdmin(adminId);

        CancelScheduleBatchCommand command = CancelScheduleBatchCommand.builder()
                .coachId(input.coachId())
                .startDate(input.startDate())
                .endDate(input.endDate())
                .type(input.type())
                .build();

        groupSchedulePort.cancelScheduleBatch(groupId, command);

        log.info("Cancelled schedule batch for group {} by admin {}", groupId, adminId);
    }

    @Transactional
    public void updateGroupSchedule(
            UUID adminId,
            UUID groupId,
            UpdateScheduleBatchCommand command
    ) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());

        groupSchedulePort.updateScheduleBatch(groupId, command);

        log.info(
                "Admin {} updated schedule for group {}",
                adminId, groupId
        );
    }

    @Transactional(readOnly = true)
    public ScheduleValidationResult validateGroupSchedule(
            UUID adminId,
            UUID groupId,
            GroupScheduleValidationCommand command
    ) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());
        return groupScheduleValidationService.validate(groupId, command);
    }

    /* ================= AVAILABILITY ================= */

    @Transactional(readOnly = true)
    public List<CoachBusySlotView> getCoachAvailability(
            UUID adminId,
            UUID coachId,
            LocalDate from,
            LocalDate to
    ) {
        verifyAdmin(adminId);

        return coachAvailabilityPort.getCoachAvailability(coachId, from, to);
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotOutput> getGroupAvailableSlots(
            UUID adminId,
            UUID groupId,
            LocalDate date
    ) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());

        return groupSchedulePort.getActiveSchedulesByGroup(groupId, date).stream()
                .map(slot -> new AvailableSlotOutput(date, slot.startTime(), slot.endTime()))
                .distinct()
                .toList();
    }

    /* ================= INTERNAL ================= */

    private void verifyAdmin(UUID adminId) {
        adminService.findById(adminId)
                .orElseThrow(() ->
                        new NotFoundException("Admin not found", adminId)
                );
    }

    private void verifyAdminBranchAccess(UUID adminId, UUID branchId) {
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }

    private void verifyCoach(UUID coachId) {
        if (!coachPort.verifyCoach(coachId)) {
            throw new NotFoundException("Coach not found", coachId);
        }
    }

    private GroupView buildGroupView(GroupDto group) {
        List<GroupCoachDto> coaches = new ArrayList<>(groupCoachPort.getActiveCoaches(group.groupId()));
        List<GroupScheduleDto> schedules = groupSchedulePort.getActiveSchedulesByGroup(group.groupId());
        int studentsCount = countStudents(group.groupId());
        OffsetDateTime nextSessionAt = toOffsetDateTime(calculateNextSession(schedules));
        List<AdminGroupHealthOutput.IssueItem> issues = buildIssues(group, coaches, schedules, studentsCount, nextSessionAt);
        GroupHealth health = resolveHealth(group.status(), issues);

        return new GroupView(
                group,
                studentsCount,
                coaches.size(),
                schedules.size(),
                !schedules.isEmpty(),
                nextSessionAt,
                health,
                issues
        );
    }

    private int countByStatus(List<GroupView> views, GroupStatus status) {
        return (int) views.stream()
                .filter(view -> view.group().status() == status)
                .count();
    }

    private int countStudents(UUID groupId) {
        return clientPort.getGroupMembers(groupId).size();
    }

    private AdminGroupMemberOutput.Capabilities buildMemberCapabilities(GroupMemberDto member) {
        boolean mutableMembership = member.contractStatus() != null
                && ("ACTIVE".equalsIgnoreCase(member.contractStatus())
                || "UPCOMING".equalsIgnoreCase(member.contractStatus()));
        return new AdminGroupMemberOutput.Capabilities(
                mutableMembership,
                mutableMembership
        );
    }

    private List<AdminGroupHealthOutput.IssueItem> buildIssues(
            GroupDto group,
            List<GroupCoachDto> coaches,
            List<GroupScheduleDto> schedules,
            int studentsCount,
            OffsetDateTime nextSessionAt
    ) {
        List<AdminGroupHealthOutput.IssueItem> issues = new ArrayList<>();

        boolean hasMainCoach = coaches.stream().anyMatch(coach -> coach.role() == CoachRole.MAIN);
        if (!hasMainCoach) {
            issues.add(new AdminGroupHealthOutput.IssueItem(
                    GroupIssueCode.NO_MAIN_COACH,
                    "В группе нет главного тренера"
            ));
        }

        if (schedules.isEmpty()) {
            issues.add(new AdminGroupHealthOutput.IssueItem(
                    GroupIssueCode.NO_SCHEDULE_PERIOD,
                    "У группы нет активного расписания"
            ));
        }

        if (!schedules.isEmpty() && nextSessionAt == null) {
            issues.add(new AdminGroupHealthOutput.IssueItem(
                    GroupIssueCode.NO_UPCOMING_SESSION,
                    "У группы нет ближайшего занятия"
            ));
        }

        int capacity = Optional.ofNullable(group.capacity()).orElse(0);
        if (capacity > 0 && studentsCount > capacity) {
            issues.add(new AdminGroupHealthOutput.IssueItem(
                    GroupIssueCode.OVER_CAPACITY,
                    "Количество игроков превышает лимит группы"
            ));
        }

        return issues;
    }

    private GroupHealth resolveHealth(GroupStatus status, List<AdminGroupHealthOutput.IssueItem> issues) {
        if (status == GroupStatus.STOPPED) {
            return GroupHealth.STOPPED;
        }
        if (status == GroupStatus.PAUSED) {
            return GroupHealth.PAUSED;
        }
        if (issues.stream().anyMatch(issue -> issue.code() == GroupIssueCode.OVER_CAPACITY)) {
            return GroupHealth.OVER_CAPACITY;
        }
        if (issues.stream().anyMatch(issue ->
                issue.code() == GroupIssueCode.NO_SCHEDULE_PERIOD || issue.code() == GroupIssueCode.NO_UPCOMING_SESSION)) {
            return GroupHealth.NO_SCHEDULE;
        }
        if (issues.stream().anyMatch(issue -> issue.code() == GroupIssueCode.NO_MAIN_COACH)) {
            return GroupHealth.NO_COACH;
        }
        return GroupHealth.OK;
    }

    private List<String> buildRecommendedActions(GroupView view) {
        List<String> actions = new ArrayList<>();
        if (view.issues().stream().anyMatch(issue -> issue.code() == GroupIssueCode.NO_MAIN_COACH)) {
            actions.add("ASSIGN_MAIN_COACH");
        }
        if (view.issues().stream().anyMatch(issue ->
                issue.code() == GroupIssueCode.NO_SCHEDULE_PERIOD || issue.code() == GroupIssueCode.NO_UPCOMING_SESSION)) {
            actions.add("CHECK_SCHEDULE");
        }
        if (view.issues().stream().anyMatch(issue -> issue.code() == GroupIssueCode.OVER_CAPACITY)) {
            actions.add("REVIEW_CAPACITY");
        }
        return actions;
    }

    private AdminGroupDetailsOutput.Capabilities buildCapabilities(GroupStatus status) {
        return new AdminGroupDetailsOutput.Capabilities(
                true,
                status == GroupStatus.ACTIVE,
                status == GroupStatus.PAUSED,
                status != GroupStatus.STOPPED,
                status != GroupStatus.STOPPED
        );
    }

    private int countScheduleConflicts(UUID groupId, List<GroupScheduleDto> schedules) {
        int conflicts = 0;
        for (GroupScheduleDto schedule : schedules) {
            List<GroupScheduleDto> coachSchedules =
                    groupSchedulePort.getActiveSchedulesByCoachAndDay(schedule.coachId(), schedule.dayOfWeek());

            boolean hasConflict = coachSchedules.stream().anyMatch(other ->
                    !other.scheduleId().equals(schedule.scheduleId())
                            && !other.groupId().equals(groupId)
                            && overlaps(schedule.startTime(), schedule.endTime(), other.startTime(), other.endTime())
                            && overlaps(schedule.startDate(), schedule.endDate(), other.startDate(), other.endDate())
            );

            if (hasConflict) {
                conflicts++;
            }
        }
        return conflicts;
    }

    private int distinctScheduleDays(List<GroupScheduleDto> schedules) {
        return (int) schedules.stream()
                .map(GroupScheduleDto::dayOfWeek)
                .distinct()
                .count();
    }

    private LocalDateTime calculateNextSession(List<GroupScheduleDto> schedules) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        return schedules.stream()
                .map(schedule -> resolveNextSessionDateTime(schedule, today, now))
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime resolveNextSessionDateTime(GroupScheduleDto schedule, LocalDate today, LocalDateTime now) {
        LocalDate candidate = today;
        while (candidate.getDayOfWeek() != schedule.dayOfWeek()) {
            candidate = candidate.plusDays(1);
        }

        if (candidate.isBefore(schedule.startDate())) {
            candidate = schedule.startDate();
            while (candidate.getDayOfWeek() != schedule.dayOfWeek()) {
                candidate = candidate.plusDays(1);
            }
        }

        LocalDateTime next = LocalDateTime.of(candidate, schedule.startTime());
        if (next.isBefore(now)) {
            next = next.plusWeeks(1);
        }

        if (next.toLocalDate().isAfter(schedule.endDate())) {
            return null;
        }

        return next;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private boolean overlaps(LocalDate startOne, LocalDate endOne, LocalDate startTwo, LocalDate endTwo) {
        return !startOne.isAfter(endTwo) && !startTwo.isAfter(endOne);
    }

    private boolean overlaps(java.time.LocalTime startOne, java.time.LocalTime endOne,
                             java.time.LocalTime startTwo, java.time.LocalTime endTwo) {
        return startOne.isBefore(endTwo) && startTwo.isBefore(endOne);
    }

    private record GroupView(
            GroupDto group,
            int studentsCount,
            int coachesCount,
            int sessionsPerWeek,
            boolean scheduleActive,
            OffsetDateTime nextSessionAt,
            GroupHealth health,
            List<AdminGroupHealthOutput.IssueItem> issues
    ) {
        private boolean overCapacity() {
            int capacity = Optional.ofNullable(group.capacity()).orElse(0);
            return capacity > 0 && studentsCount > capacity;
        }

        private int occupancyPercent() {
            int capacity = Optional.ofNullable(group.capacity()).orElse(0);
            if (capacity <= 0) {
                return 0;
            }
            return Math.min(100, (int) Math.round((studentsCount * 100.0) / capacity));
        }

        private AdminGroupOverviewOutput.GroupItem toOverviewItem() {
            return new AdminGroupOverviewOutput.GroupItem(
                    group.groupId(),
                    group.name(),
                    group.status(),
                    group.ageFrom(),
                    group.ageTo(),
                    group.level(),
                    group.capacity(),
                    studentsCount,
                    coachesCount,
                    scheduleActive,
                    nextSessionAt,
                    health
            );
        }
    }

}
