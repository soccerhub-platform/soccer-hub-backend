package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.coach.*;
import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatus;
import kz.edu.soccerhub.coach.domain.model.enums.WorkStatus;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCoachService {

    private static final int OVERVIEW_MAX_SLOTS = 12;
    private static final int OVERVIEW_HIGH_LOAD_PERCENT = 75;

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
        coachPort.recordStatusHistory(coachId, CoachStatus.ACTIVE, adminId);
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
        ensureAdminHasCoachAccess(adminId, coachId);

        AccountStatus accountStatus = resolveAccountStatus(input);
        WorkStatus workStatus = resolveWorkStatus(input);

        if (accountStatus != null) {
            switch (accountStatus) {
                case ACTIVE -> coachPort.enableCoach(coachId);
                case INACTIVE -> coachPort.disableCoach(coachId);
            }
            coachPort.recordStatusHistory(coachId, legacyStatus(accountStatus), adminId);
        }

        if (workStatus != null) {
            coachPort.updateWorkStatus(
                    coachId,
                    workStatus,
                    input.vacationFrom(),
                    input.vacationTo(),
                    trimToNull(input.reason())
            );
            coachPort.recordStatusHistory(coachId, legacyStatus(workStatus), adminId);
        }
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
    public AdminCoachOverviewOutput     getCoachesOverview(
            UUID adminId,
            UUID branchId,
            int page,
            int size,
            String search,
            String status,
            List<String> sort
    ) {
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
        validateOverviewPage(page, size);

        AdminCoachOverviewStatus filterStatus = AdminCoachOverviewStatus.fromValue(status);
        AdminCoachOverviewOutput overview = buildOverview(branchId);
        List<AdminCoachOverviewOutput.CoachItem> filtered = overview.coaches().getContent().stream()
                .filter(item -> matchesSearch(item, search))
                .filter(item -> matchesStatus(item, filterStatus))
                .sorted(buildOverviewComparator(sort))
                .toList();

        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());

        return new AdminCoachOverviewOutput(
                overview.summary(),
                new PageImpl<>(
                        filtered.subList(fromIndex, toIndex),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        filtered.size()
                )
        );
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
                    Page.empty()
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

            String loadStatus = resolveOverviewLoadStatus(weeklyCount, OVERVIEW_MAX_SLOTS);
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
                    coach.accountStatus() == null ? null : coach.accountStatus().name(),
                    coach.workStatus() == null ? null : coach.workStatus().name(),
                    coach.vacationFrom(),
                    coach.vacationTo(),
                    coach.workStatusReason(),
                    coach.specialization(),
                    coach.createdAt(),
                    coachGroups,
                    weeklyCount,
                    todayCount,
                    new AdminCoachOverviewOutput.Load(weeklyCount, OVERVIEW_MAX_SLOTS, loadStatus),
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
                new PageImpl<>(items)
        );
    }

    private void validateOverviewPage(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page must not be negative", page);
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Size must be between 1 and 100", size);
        }
    }

    private boolean matchesSearch(AdminCoachOverviewOutput.CoachItem item, String search) {
        String normalized = trimToNull(search);
        if (normalized == null) {
            return true;
        }

        String needle = normalized.toLowerCase(Locale.ROOT);
        String fullName = (item.firstName() + " " + item.lastName()).trim();
        return contains(item.firstName(), needle)
               || contains(item.lastName(), needle)
               || contains(fullName, needle)
               || contains(item.email(), needle)
               || contains(item.phone(), needle);
    }

    private boolean matchesStatus(
            AdminCoachOverviewOutput.CoachItem item,
            AdminCoachOverviewStatus status
    ) {
        return switch (status) {
            case ALL -> true;
            case ACTIVE -> item.active();
            case INACTIVE -> !item.active();
            case WITHOUT_GROUPS -> item.groups().isEmpty();
            case OVERLOADED -> "HIGH".equals(item.load().status())
                               || "OVERLOADED".equals(item.load().status())
                               || item.load().usedSlots() > item.load().maxSlots();
            case TODAY -> item.todaySessionsCount() > 0;
        };
    }

    private Comparator<AdminCoachOverviewOutput.CoachItem> buildOverviewComparator(List<String> sortParams) {
        List<SortInstruction> instructions = parseSortInstructions(sortParams);
        Comparator<AdminCoachOverviewOutput.CoachItem> comparator = null;

        for (SortInstruction instruction : instructions) {
            Comparator<AdminCoachOverviewOutput.CoachItem> next = comparatorFor(instruction.key());
            if (!instruction.ascending()) {
                next = next.reversed();
            }
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }

        return comparator == null ? defaultOverviewComparator() : comparator;
    }

    private List<SortInstruction> parseSortInstructions(List<String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return List.of(
                    new SortInstruction("active", false),
                    new SortInstruction("lastName", true),
                    new SortInstruction("firstName", true),
                    new SortInstruction("coachId", true)
            );
        }

        List<String> normalizedSortParams = normalizeSortParams(sortParams);
        List<SortInstruction> instructions = new ArrayList<>();
        for (String sortParam : normalizedSortParams) {
            String value = trimToNull(sortParam);
            if (value == null) {
                continue;
            }

            String[] parts = value.split(",", 2);
            String key = parts[0].trim();
            String direction = parts.length > 1 ? parts[1].trim() : "asc";
            if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
                throw new BadRequestException("Unknown sort direction", direction);
            }

            ensureSortableKey(key);
            instructions.add(new SortInstruction(key, "asc".equalsIgnoreCase(direction)));
        }

        if (instructions.isEmpty()) {
            return parseSortInstructions(null);
        }

        boolean hasLastName = instructions.stream().anyMatch(item -> item.key().equals("lastName"));
        boolean hasFirstName = instructions.stream().anyMatch(item -> item.key().equals("firstName"));
        boolean hasCoachId = instructions.stream().anyMatch(item -> item.key().equals("coachId"));

        if (!hasLastName) {
            instructions.add(new SortInstruction("lastName", true));
        }
        if (!hasFirstName) {
            instructions.add(new SortInstruction("firstName", true));
        }
        if (!hasCoachId) {
            instructions.add(new SortInstruction("coachId", true));
        }
        return List.copyOf(instructions);
    }

    private List<String> normalizeSortParams(List<String> sortParams) {
        List<String> normalized = new ArrayList<>();

        for (int index = 0; index < sortParams.size(); index++) {
            String current = trimToNull(sortParams.get(index));
            if (current == null) {
                continue;
            }

            if (current.contains(",")) {
                normalized.add(current);
                continue;
            }

            if (index + 1 < sortParams.size()) {
                String next = trimToNull(sortParams.get(index + 1));
                if (next != null && isSortDirection(next)) {
                    normalized.add(current + "," + next);
                    index++;
                    continue;
                }
            }

            normalized.add(current);
        }

        return List.copyOf(normalized);
    }

    private boolean isSortDirection(String value) {
        return "asc".equalsIgnoreCase(value) || "desc".equalsIgnoreCase(value);
    }

    private void ensureSortableKey(String key) {
        Set<String> allowed = Set.of(
                "coachId",
                "firstName",
                "lastName",
                "email",
                "phone",
                "active",
                "groupsCount",
                "todaySessionsCount",
                "weeklySessionsCount",
                "loadUsed",
                "loadMax",
                "loadPercent",
                "loadStatus",
                "lastReportAt",
                "overdueReportsCount",
                "createdAt"
        );
        if (!allowed.contains(key)) {
            throw new BadRequestException("Unknown coach overview sort key", key);
        }
    }

    private Comparator<AdminCoachOverviewOutput.CoachItem> comparatorFor(String key) {
        return switch (key) {
            case "coachId" -> Comparator.comparing(AdminCoachOverviewOutput.CoachItem::coachId);
            case "firstName" -> Comparator.comparing(AdminCoachOverviewOutput.CoachItem::firstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "lastName" -> Comparator.comparing(AdminCoachOverviewOutput.CoachItem::lastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "email" -> Comparator.comparing(AdminCoachOverviewOutput.CoachItem::email, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "phone" -> Comparator.comparing(AdminCoachOverviewOutput.CoachItem::phone, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "active" -> Comparator.comparing(AdminCoachOverviewOutput.CoachItem::active);
            case "groupsCount" -> Comparator.comparingInt(item -> item.groups().size());
            case "todaySessionsCount" -> Comparator.comparingInt(AdminCoachOverviewOutput.CoachItem::todaySessionsCount);
            case "weeklySessionsCount" -> Comparator.comparingInt(AdminCoachOverviewOutput.CoachItem::weeklySessionsCount);
            case "loadUsed" -> Comparator.comparingInt(item -> item.load().usedSlots());
            case "loadMax" -> Comparator.comparingInt(item -> item.load().maxSlots());
            case "loadPercent" -> Comparator.comparingInt(this::loadPercent);
            case "loadStatus" -> Comparator.comparingInt(item -> loadStatusRank(item.load().status()));
            case "lastReportAt" -> Comparator.comparing(item -> item.reports().lastReportAt(), Comparator.nullsLast(Comparator.naturalOrder()));
            case "overdueReportsCount" -> Comparator.comparingInt(item -> item.reports().overdueCount());
            case "createdAt" -> Comparator.comparing(this::coachCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> throw new BadRequestException("Unknown coach overview sort key", key);
        };
    }

    private Comparator<AdminCoachOverviewOutput.CoachItem> defaultOverviewComparator() {
        return Comparator.comparing(AdminCoachOverviewOutput.CoachItem::active).reversed()
                .thenComparing(AdminCoachOverviewOutput.CoachItem::lastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(AdminCoachOverviewOutput.CoachItem::firstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(AdminCoachOverviewOutput.CoachItem::coachId);
    }

    private int loadPercent(AdminCoachOverviewOutput.CoachItem item) {
        if (item.load().maxSlots() <= 0) {
            return 0;
        }
        return (int) Math.round(item.load().usedSlots() * 100.0 / item.load().maxSlots());
    }

    private int loadStatusRank(String status) {
        if (status == null) {
            return Integer.MAX_VALUE;
        }

        return switch (status) {
            case "NORMAL" -> 0;
            case "HIGH" -> 1;
            case "OVERLOADED" -> 2;
            default -> 3;
        };
    }

    private LocalDateTime coachCreatedAt(AdminCoachOverviewOutput.CoachItem item) {
        return item.createdAt();
    }

    private String resolveOverviewLoadStatus(int usedSlots, int maxSlots) {
        if (usedSlots > maxSlots) {
            return "OVERLOADED";
        }

        int percent = maxSlots <= 0 ? 0 : (int) Math.round(usedSlots * 100.0 / maxSlots);
        if (percent >= OVERVIEW_HIGH_LOAD_PERCENT) {
            return "HIGH";
        }
        return "NORMAL";
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
        String coachName = formatCoachName(coach.firstName(), coach.lastName());
        List<AdminCoachProfileOutput.WeeklyScheduleItem> weeklySchedule = schedules.stream()
                .map(schedule -> {
                    GroupDto group = groupsById.get(schedule.groupId());
                    String groupName = group == null ? "Unknown group" : group.name();
                    return new AdminCoachProfileOutput.WeeklyScheduleItem(
                            schedule.scheduleId(),
                            schedule.dayOfWeek(),
                            schedule.startTime(),
                            schedule.endTime(),
                            schedule.status(),
                            resolveScheduleStatusLabel(schedule.status()),
                            schedule.groupId(),
                            groupName,
                            coachName,
                            schedule.startDate(),
                            schedule.endDate(),
                            buildScheduleConflicts(schedule, allActiveSchedules, groupsById, coachId, coachName)
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
                coach.accountStatus() == null ? null : coach.accountStatus().name(),
                coach.workStatus() == null ? null : coach.workStatus().name(),
                coach.vacationFrom(),
                coach.vacationTo(),
                coach.workStatusReason(),
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

    private List<AdminCoachProfileOutput.ScheduleConflictItem> buildScheduleConflicts(
            GroupScheduleDto schedule,
            List<GroupScheduleDto> allActiveSchedules,
            Map<UUID, GroupDto> groupsById,
            UUID coachId,
            String coachName
    ) {
        return allActiveSchedules.stream()
                .filter(other -> !other.scheduleId().equals(schedule.scheduleId()))
                .filter(other -> !other.groupId().equals(schedule.groupId()))
                .filter(other -> other.dayOfWeek() == schedule.dayOfWeek())
                .filter(other -> overlaps(schedule.startTime(), schedule.endTime(), other.startTime(), other.endTime()))
                .filter(other -> overlaps(schedule.startDate(), schedule.endDate(), other.startDate(), other.endDate()))
                .map(other -> {
                    GroupDto conflictingGroup = groupsById.get(other.groupId());
                    return new AdminCoachProfileOutput.ScheduleConflictItem(
                            other.dayOfWeek(),
                            other.startTime(),
                            other.endTime(),
                            coachId,
                            coachName,
                            other.groupId(),
                            conflictingGroup == null ? "Unknown group" : conflictingGroup.name()
                    );
                })
                .toList();
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

    private String formatCoachName(String firstName, String lastName) {
        return java.util.stream.Stream.of(firstName, lastName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String resolveScheduleStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status) {
            case "ACTIVE" -> "Активно";
            case "INACTIVE" -> "Неактивно";
            case "CANCELLED" -> "Отменено";
            case "DRAFT" -> "Черновик";
            default -> status;
        };
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private AccountStatus resolveAccountStatus(AdminCoachUpdateCoachStatusInput input) {
        if (input.accountStatus() != null) {
            return input.accountStatus();
        }
        if (input.status() == null) {
            return null;
        }
        return switch (input.status()) {
            case ACTIVE -> AccountStatus.ACTIVE;
            case INACTIVE -> AccountStatus.INACTIVE;
            case BUSY, VACATION -> null;
        };
    }

    private WorkStatus resolveWorkStatus(AdminCoachUpdateCoachStatusInput input) {
        if (input.workStatus() != null) {
            return input.workStatus();
        }
        if (input.status() == null) {
            return null;
        }
        return switch (input.status()) {
            case ACTIVE, INACTIVE -> null;
            case BUSY -> WorkStatus.BUSY;
            case VACATION -> WorkStatus.VACATION;
        };
    }

    private CoachStatus legacyStatus(AccountStatus accountStatus) {
        return accountStatus == AccountStatus.ACTIVE ? CoachStatus.ACTIVE : CoachStatus.INACTIVE;
    }

    private CoachStatus legacyStatus(WorkStatus workStatus) {
        return switch (workStatus) {
            case AVAILABLE -> CoachStatus.ACTIVE;
            case BUSY -> CoachStatus.BUSY;
            case VACATION -> CoachStatus.VACATION;
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean overlaps(LocalDate startOne, LocalDate endOne, LocalDate startTwo, LocalDate endTwo) {
        return !startOne.isAfter(endTwo) && !startTwo.isAfter(endOne);
    }

    private boolean overlaps(LocalTime startOne, LocalTime endOne, LocalTime startTwo, LocalTime endTwo) {
        return startOne.isBefore(endTwo) && startTwo.isBefore(endOne);
    }

    private record SortInstruction(
            String key,
            boolean ascending
    ) {
    }

    public AdminCoachResetPasswordOutput resetCoachPassword(UUID adminId, UUID coachId) {
        String tempPassword = passwordGenerator.generate(8);
        authPort.resetPassword(coachId, tempPassword);

        log.info("Coach password reset: coachId={}, adminId={}", coachId, adminId);
        return new AdminCoachResetPasswordOutput(tempPassword);
    }
}
