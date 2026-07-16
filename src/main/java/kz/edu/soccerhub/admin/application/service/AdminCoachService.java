package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.coach.*;
import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatus;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatusHistoryEventType;
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
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.dto.media.MediaDownloadUrlResponse;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.dispatcher.application.service.PasswordGenerator;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.enums.MediaVariant;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final MediaAvatarPort mediaAvatarPort;
    private final MediaAccessPort mediaAccessPort;

    @Transactional
    public MediaAssetResponse uploadCoachAvatar(UUID adminId, UUID coachId, MultipartFile file) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        ensureAdminHasCoachAccess(adminId, coachId);
        MediaAsset avatar = mediaAvatarPort.uploadAvatar(MediaOwnerType.COACH, coachId, adminId, file);
        return mediaAccessPort.toResponse(avatar);
    }

    @Transactional
    public void deleteCoachAvatar(UUID adminId, UUID coachId) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        ensureAdminHasCoachAccess(adminId, coachId);
        mediaAvatarPort.deleteAvatar(MediaOwnerType.COACH, coachId, adminId);
    }

    @Transactional(readOnly = true)
    public MediaDownloadUrlResponse getCoachAvatarDownloadUrl(UUID adminId, UUID coachId) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        ensureAdminHasCoachAccess(adminId, coachId);
        MediaAsset avatar = mediaAvatarPort.findActiveAvatar(MediaOwnerType.COACH, coachId)
                .orElseThrow(() -> new NotFoundException("Coach avatar not found", coachId));
        return new MediaDownloadUrlResponse(mediaAccessPort.createContentUrl(avatar, MediaVariant.ORIGINAL));
    }

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
                .birthDate(input.birthDate())
                .phone(input.phone())
                .bio(input.description())
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

        CoachDto currentCoach = coachPort.getCoach(coachId);
        AccountStatus accountStatus = resolveAccountStatus(input);
        WorkStatus workStatus = resolveWorkStatus(input);
        String reason = trimToNull(input.reason());

        AccountStatus previousAccountStatus = currentCoach.accountStatus();
        WorkStatus previousWorkStatus = currentCoach.workStatus();
        boolean accountChanged = accountStatus != null && accountStatus != previousAccountStatus;
        boolean workChanged = workStatus != null && workStatus != previousWorkStatus;

        if (!accountChanged && !workChanged) {
            return;
        }

        if (accountChanged) {
            updateAccountStatus(coachId, accountStatus);
        }

        if (workChanged) {
            coachPort.updateWorkStatus(
                    coachId,
                    workStatus,
                    input.vacationFrom(),
                    input.vacationTo(),
                    reason
            );
        }

        coachPort.recordStatusHistory(
                coachId,
                legacyStatus(accountStatus, workStatus),
                adminId,
                resolveStatusHistoryEventType(accountChanged, previousWorkStatus, workStatus),
                accountChanged ? previousAccountStatus : null,
                accountChanged ? accountStatus : null,
                workChanged ? previousWorkStatus : null,
                workChanged ? workStatus : null,
                reason,
                workStatus == WorkStatus.VACATION ? input.vacationFrom() : null,
                workStatus == WorkStatus.VACATION ? input.vacationTo() : null
        );
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
                .birthDate(input.birthDate())
                .phone(input.phone())
                .specialization(input.specialization())
                .bio(input.description())
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

    @Transactional(readOnly = true)
    public AdminTrainerListOutput getTrainers(
            UUID adminId,
            UUID branchId,
            String search,
            List<AccountStatus> accountStatuses,
            List<WorkStatus> workStatuses,
            AdminTrainerFilterEnums.GroupFilter groupFilter,
            AdminTrainerFilterEnums.WorkloadStatus workloadStatus,
            AdminTrainerFilterEnums.ReportStatus reportStatus,
            Boolean hasSessionToday,
            AdminTrainerFilterEnums.SortField sort,
            org.springframework.data.domain.Sort.Direction direction,
            int page,
            int size
    ) {
        verifyAdminAccessToBranch(adminId, branchId);
        validateOverviewPage(page, size);

        List<AdminTrainerListOutput.Item> filtered = buildTrainerRows(branchId).stream()
                .filter(item -> matchesTrainerSearch(item, search))
                .filter(item -> accountStatuses == null || accountStatuses.isEmpty() || accountStatuses.stream().map(Enum::name).anyMatch(item.accountStatus()::equals))
                .filter(item -> workStatuses == null || workStatuses.isEmpty() || workStatuses.stream().map(Enum::name).anyMatch(item.workStatus()::equals))
                .filter(item -> matchesGroupFilter(item, groupFilter))
                .filter(item -> workloadStatus == null || workloadStatus.name().equals(item.load().status()))
                .filter(item -> matchesReportFilter(item, reportStatus))
                .filter(item -> hasSessionToday == null || item.todaySessionsCount() > 0 == hasSessionToday)
                .sorted(trainerComparator(sort, direction))
                .toList();

        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        return new AdminTrainerListOutput(
                filtered.subList(fromIndex, toIndex),
                page,
                size,
                filtered.size(),
                size == 0 ? 0 : (int) Math.ceil(filtered.size() / (double) size)
        );
    }

    @Transactional(readOnly = true)
    public AdminTrainerSummaryOutput getTrainersSummary(UUID adminId, UUID branchId) {
        verifyAdminAccessToBranch(adminId, branchId);
        List<AdminTrainerListOutput.Item> rows = buildTrainerRows(branchId);
        return new AdminTrainerSummaryOutput(
                rows.size(),
                (int) rows.stream().filter(item -> "ACTIVE".equals(item.accountStatus())).count(),
                (int) rows.stream().filter(item -> "INACTIVE".equals(item.accountStatus())).count(),
                (int) rows.stream().filter(item -> item.groupCount() == 0).count(),
                (int) rows.stream().filter(item -> "OVERLOADED".equals(item.load().status())).count(),
                (int) rows.stream().filter(item -> item.todaySessionsCount() > 0).count()
        );
    }

    @Transactional(readOnly = true)
    public AdminTrainerOverviewOutput getTrainerOverview(UUID adminId, UUID trainerId) {
        Set<UUID> allowedBranchIds = adminBranchService.getAdminBranches(adminId).stream()
                .map(kz.edu.soccerhub.admin.application.dto.branch.AdminBranchesOutput::branchId)
                .collect(Collectors.toSet());
        AdminCoachProfileOutput profile = buildProfile(trainerId, allowedBranchIds);
        CoachDto coach = coachPort.getCoach(trainerId);
        List<GroupCoachDto> groupLinks = new ArrayList<>(groupCoachPort.getActiveAssignmentsByCoachId(trainerId));
        Set<UUID> groupIds = groupLinks.stream().map(GroupCoachDto::groupId).collect(Collectors.toSet());
        Map<UUID, GroupDto> groupsById = groupPort.getGroupsByIds(groupIds).stream()
                .collect(Collectors.toMap(GroupDto::groupId, group -> group));
        LocalDate today = LocalDate.now();
        List<CoachSessionAdminView> upcoming = coachPort.getUpcomingSessions(trainerId, today);
        AdminTrainerOverviewOutput.NextSession nextSession = upcoming.stream()
                .filter(session -> !"CANCELLED".equals(session.status()))
                .min(Comparator.comparing(CoachSessionAdminView::sessionDate).thenComparing(CoachSessionAdminView::scheduledStartAt))
                .map(session -> toTrainerNextSession(session, groupsById))
                .orElse(null);
        AdminTrainerOverviewOutput.LastReport lastReport = coachPort.getReportedSessions(trainerId).stream()
                .filter(session -> session.updatedAt() != null)
                .findFirst()
                .map(session -> toTrainerLastReport(session, groupsById))
                .orElse(null);
        List<AdminTrainerOverviewOutput.GroupItem> groups = groupLinks.stream()
                .map(link -> {
                    GroupDto group = groupsById.get(link.groupId());
                    return new AdminTrainerOverviewOutput.GroupItem(
                            link.groupId(),
                            group == null ? "Unknown group" : group.name(),
                            link.role() == null ? null : link.role().name()
                    );
                })
                .toList();
        kz.edu.soccerhub.coach.application.dto.profile.CoachAvailabilityResponse availability = coachPort.getAvailability(trainerId);

        return new AdminTrainerOverviewOutput(
                new AdminTrainerOverviewOutput.Trainer(
                        coach.id(),
                        coach.firstName(),
                        coach.lastName(),
                        coach.email(),
                        coach.phone(),
                        coach.specialization(),
                        coach.accountStatus() == null ? null : coach.accountStatus().name(),
                        coach.workStatus() == null ? null : coach.workStatus().name()
                ),
                new AdminTrainerListOutput.Load(
                        profile.load().completed(),
                        profile.load().planned(),
                        profile.load().used(),
                        profile.load().limit(),
                        profile.load().percentage(),
                        profile.load().status()
                ),
                buildAttentionItems(coach, profile, nextSession),
                groups,
                new AdminTrainerOverviewOutput.Availability(
                        availability.days(),
                        availability.timeFrom(),
                        availability.timeTo(),
                        availability.timezone()
                ),
                nextSession,
                lastReport
        );
    }

    @Transactional(readOnly = true)
    public kz.edu.soccerhub.coach.application.dto.profile.CoachAvailabilityResponse getTrainerAvailability(UUID adminId, UUID trainerId) {
        ensureAdminHasCoachAccess(adminId, trainerId);
        return coachPort.getAvailability(trainerId);
    }

    @Transactional
    public kz.edu.soccerhub.coach.application.dto.profile.CoachAvailabilityResponse updateTrainerAvailability(
            UUID adminId,
            UUID trainerId,
            kz.edu.soccerhub.coach.application.dto.profile.CoachAvailabilityUpdateRequest request
    ) {
        ensureAdminHasCoachAccess(adminId, trainerId);
        return coachPort.updateAvailability(trainerId, request);
    }

    @Transactional
    public AdminTrainerGroupAssignmentOutput assignTrainerToGroup(UUID adminId, UUID trainerId, AdminTrainerGroupAssignmentInput input) {
        ensureAdminHasCoachAccess(adminId, trainerId);
        GroupDto group = groupPort.getGroupById(input.groupId());
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, group.branchId())) {
            throw new BadRequestException("Admin does not have access to branch", group.branchId());
        }
        if (input.assignedTo() != null && input.assignedFrom() != null && input.assignedTo().isBefore(input.assignedFrom())) {
            throw new BadRequestException("assignedTo must not be before assignedFrom", input);
        }
        ensureNoTrainerScheduleConflicts(trainerId, input.groupId());
        UUID assignmentId = groupCoachPort.assignCoach(
                input.groupId(),
                trainerId,
                input.role(),
                input.assignedFrom(),
                input.assignedTo()
        );
        return new AdminTrainerGroupAssignmentOutput(assignmentId);
    }


    public AdminTrainerActivityOutput getTrainerActivity(UUID adminId, UUID trainerId, int page, int size) {
        ensureAdminHasCoachAccess(adminId, trainerId);
        validateOverviewPage(page, size);

        Map<UUID, AdminTrainerActivityOutput.Actor> actorsById = new HashMap<>();

        List<AdminTrainerActivityOutput.Item> events = coachPort.getStatusHistory(trainerId).stream()
                .sorted(Comparator.comparing(CoachStatusHistoryDto::changedAt).reversed())
                .map(item -> toTrainerActivityItem(trainerId, item, actorsById))
                .toList();

        int fromIndex = Math.min(page * size, events.size());
        int toIndex = Math.min(fromIndex + size, events.size());
        List<AdminTrainerActivityOutput.Item> content = events.subList(fromIndex, toIndex);

        int totalPages = events.isEmpty()
                ? 0
                : (int) Math.ceil((double) events.size() / size);

        return new AdminTrainerActivityOutput(
                content,
                events.size(),
                totalPages,
                page,
                size,
                page == 0,
                totalPages == 0 || page >= totalPages - 1,
                content.isEmpty()
        );
    }

    private AdminTrainerActivityOutput.Item toTrainerActivityItem(
            UUID trainerId,
            CoachStatusHistoryDto item,
            Map<UUID, AdminTrainerActivityOutput.Actor> actorsById
    ) {
        List<AdminTrainerActivityOutput.Change> changes = trainerActivityChanges(item);
        String type = trainerActivityType(item, changes);

        return new AdminTrainerActivityOutput.Item(
                buildTrainerActivityId(trainerId, item),
                item.changedAt().atOffset(ZoneOffset.UTC),
                type,
                trainerActivityTitle(type, changes),
                resolveTrainerActivityActor(item.changedBy(), actorsById),
                changes,
                trainerActivityMetadata(item, changes)
        );
    }

    private String trainerActivityType(
            CoachStatusHistoryDto item,
            List<AdminTrainerActivityOutput.Change> changes
    ) {
        AdminTrainerActivityOutput.Change workStatusChange = changes.stream()
                .filter(change -> "workStatus".equals(change.field()))
                .findFirst()
                .orElse(null);

        if (workStatusChange != null && "VACATION".equals(workStatusChange.to())) {
            return "VACATION_SET";
        }

        if (workStatusChange != null && "VACATION".equals(workStatusChange.from())
                && !"VACATION".equals(workStatusChange.to())) {
            return "VACATION_ENDED";
        }

        boolean accountChanged = changes.stream()
                .anyMatch(change -> "accountStatus".equals(change.field()));

        boolean workChanged = changes.stream()
                .anyMatch(change -> "workStatus".equals(change.field()));

        if (accountChanged && workChanged) {
            return "TRAINER_STATUS_CHANGED";
        }

        if (accountChanged) {
            return "ACCOUNT_STATUS_CHANGED";
        }

        if (workChanged) {
            return "WORK_STATUS_CHANGED";
        }

        return item.eventType() == null ? "TRAINER_STATUS_CHANGED" : item.eventType();
    }

    private Map<String, Object> trainerActivityMetadata(
            CoachStatusHistoryDto item,
            List<AdminTrainerActivityOutput.Change> changes
    ) {
        boolean vacationEvent = changes.stream()
                .anyMatch(change -> "workStatus".equals(change.field())
                        && ("VACATION".equals(change.from()) || "VACATION".equals(change.to())));

        if (!vacationEvent) {
            return Map.of();
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("vacationFrom", item.vacationFrom());
        metadata.put("vacationTo", item.vacationTo());
        metadata.put("reason", item.reason());

        return metadata;
    }

    private AdminTrainerActivityOutput.Actor resolveTrainerActivityActor(
            UUID actorId,
            Map<UUID, AdminTrainerActivityOutput.Actor> actorsById
    ) {
        if (actorId == null) {
            return systemActivityActor();
        }

        return actorsById.computeIfAbsent(actorId, id ->
                adminService.findById(id)
                        .map(admin -> new AdminTrainerActivityOutput.Actor(
                                admin.id(),
                                fullName(admin.firstName(), admin.lastName(), admin.email())
                        ))
                        .orElseGet(this::systemActivityActor)
        );
    }

    private AdminTrainerActivityOutput.Actor systemActivityActor() {
        return new AdminTrainerActivityOutput.Actor(null, "Системное событие");
    }

    private String fullName(String firstName, String lastName, String fallback) {
        String value = Stream.of(firstName, lastName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));

        if (!value.isBlank()) {
            return value;
        }

        return fallback == null || fallback.isBlank() ? "Пользователь" : fallback;
    }

    private String buildTrainerActivityId(UUID trainerId, CoachStatusHistoryDto item) {
        String changedAt = item.changedAt() == null ? "unknown-time" : item.changedAt().toString();
        String changedBy = item.changedBy() == null ? "unknown-actor" : item.changedBy().toString();
        String type = item.eventType() == null ? "STATUS_CHANGED" : item.eventType();

        return trainerId + ":" + type + ":" + changedAt + ":" + changedBy;
    }

    private String trainerActivityTitle(
            String type,
            List<AdminTrainerActivityOutput.Change> changes
    ) {
        boolean accountChanged = changes.stream()
                .anyMatch(change -> "accountStatus".equals(change.field()));

        boolean workChanged = changes.stream()
                .anyMatch(change -> "workStatus".equals(change.field()));

        AdminTrainerActivityOutput.Change workStatusChange = changes.stream()
                .filter(change -> "workStatus".equals(change.field()))
                .findFirst()
                .orElse(null);

        if (workStatusChange != null && "VACATION".equals(workStatusChange.to())) {
            return "Тренер переведен в отпуск";
        }

        if (workStatusChange != null && "VACATION".equals(workStatusChange.from())
                && !"VACATION".equals(workStatusChange.to())) {
            return "Тренер вернулся из отпуска";
        }

        if (accountChanged && workChanged) {
            return "Статусы тренера изменены";
        }

        if (accountChanged) {
            return "Статус учетной записи изменен";
        }

        if (workChanged) {
            return "Рабочий статус изменен";
        }

        return switch (type) {
            case "VACATION_SET" -> "Тренер переведен в отпуск";
            case "VACATION_ENDED" -> "Тренер вернулся из отпуска";
            default -> "Профиль тренера изменен";
        };
    }

    private List<AdminTrainerActivityOutput.Change> trainerActivityChanges(CoachStatusHistoryDto item) {
        List<AdminTrainerActivityOutput.Change> changes = new ArrayList<>();

        if (item.previousAccountStatus() != null || item.newAccountStatus() != null) {
            changes.add(new AdminTrainerActivityOutput.Change(
                    "accountStatus",
                    "Статус учетной записи",
                    item.previousAccountStatus(),
                    item.newAccountStatus(),
                    accountStatusLabel(item.previousAccountStatus()),
                    accountStatusLabel(item.newAccountStatus())
            ));
        }

        if (item.previousWorkStatus() != null || item.newWorkStatus() != null) {
            changes.add(new AdminTrainerActivityOutput.Change(
                    "workStatus",
                    "Рабочий статус",
                    item.previousWorkStatus(),
                    item.newWorkStatus(),
                    workStatusLabel(item.previousWorkStatus()),
                    workStatusLabel(item.newWorkStatus())
            ));
        }

        return changes;
    }

    private String accountStatusLabel(String status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case "ACTIVE" -> "Активен";
            case "INACTIVE" -> "Отключен";
            default -> status;
        };
    }

    private String workStatusLabel(String status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case "AVAILABLE" -> "Доступен";
            case "BUSY" -> "Занят";
            case "VACATION" -> "В отпуске";
            default -> status;
        };
    }

    private void ensureNoTrainerScheduleConflicts(UUID trainerId, UUID targetGroupId) {
        List<GroupScheduleDto> targetSchedules = groupSchedulePort.getActiveSchedulesByGroup(targetGroupId);
        List<GroupScheduleDto> coachSchedules = groupSchedulePort.getActiveSchedulesByCoach(trainerId);
        Set<UUID> coachGroupIds = coachSchedules.stream().map(GroupScheduleDto::groupId).collect(Collectors.toSet());
        Map<UUID, GroupDto> groupsById = coachGroupIds.isEmpty()
                ? Map.of()
                : groupPort.getGroupsByIds(coachGroupIds).stream().collect(Collectors.toMap(GroupDto::groupId, group -> group));
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (GroupScheduleDto target : targetSchedules) {
            for (GroupScheduleDto existing : coachSchedules) {
                if (existing.groupId().equals(targetGroupId)) {
                    continue;
                }
                if (existing.dayOfWeek() != target.dayOfWeek()) {
                    continue;
                }
                if (!overlaps(target.startDate(), target.endDate(), existing.startDate(), existing.endDate())) {
                    continue;
                }
                if (!overlaps(target.startTime(), target.endTime(), existing.startTime(), existing.endTime())) {
                    continue;
                }
                GroupDto conflictingGroup = groupsById.get(existing.groupId());
                conflicts.add(Map.of(
                        "groupId", existing.groupId(),
                        "groupName", conflictingGroup == null ? "Unknown group" : conflictingGroup.name(),
                        "startAt", target.startDate().atTime(existing.startTime()),
                        "endAt", target.startDate().atTime(existing.endTime())
                ));
            }
        }
        if (!conflicts.isEmpty()) {
            throw new ConflictException(
                    "У тренера уже есть занятие в указанное время",
                    "TRAINER_SCHEDULE_CONFLICT",
                    Map.of("conflicts", conflicts)
            );
        }
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

    private void verifyAdminAccessToBranch(UUID adminId, UUID branchId) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
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
        Map<UUID, Integer> completedByCoach = new HashMap<>();
        Map<UUID, Integer> plannedByCoach = new HashMap<>();
        Map<UUID, Integer> pendingReportByCoach = new HashMap<>();
        Map<UUID, Integer> todayByCoach = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (CoachSessionAdminView session : weekSessions) {
            if ("CANCELLED".equals(session.status())) {
                continue;
            }
            weeklyByCoach.merge(session.coachId(), 1, Integer::sum);
            if ("COMPLETED".equals(session.status())) {
                completedByCoach.merge(session.coachId(), 1, Integer::sum);
            } else {
                plannedByCoach.merge(session.coachId(), 1, Integer::sum);
            }
            if (today.equals(session.sessionDate())) {
                todayByCoach.merge(session.coachId(), 1, Integer::sum);
            }
            if ("PENDING".equals(resolveSessionReportStatus(session, now))) {
                pendingReportByCoach.merge(session.coachId(), 1, Integer::sum);
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
            int completedCount = completedByCoach.getOrDefault(coach.id(), 0);
            int plannedCount = plannedByCoach.getOrDefault(coach.id(), 0);
            int todayCount = todayByCoach.getOrDefault(coach.id(), 0);
            if (todayCount > 0) {
                withSessionsToday++;
            }

            int loadPercent = loadPercentage(weeklyCount, OVERVIEW_MAX_SLOTS);
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
                    new AdminCoachOverviewOutput.Load(
                            weeklyCount,
                            OVERVIEW_MAX_SLOTS,
                            loadStatus,
                            completedCount,
                            plannedCount,
                            weeklyCount,
                            OVERVIEW_MAX_SLOTS,
                            loadPercent
                    ),
                    new AdminCoachOverviewOutput.Reports(
                            overdueByCoach.getOrDefault(coach.id(), 0),
                            pendingReportByCoach.getOrDefault(coach.id(), 0),
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

    private List<AdminTrainerListOutput.Item> buildTrainerRows(UUID branchId) {
        AdminCoachOverviewOutput overview = buildOverview(branchId);
        return overview.coaches().getContent().stream()
                .map(item -> new AdminTrainerListOutput.Item(
                        item.coachId(),
                        item.firstName(),
                        item.lastName(),
                        item.email(),
                        item.phone(),
                        item.specialization(),
                        item.accountStatus(),
                        item.workStatus(),
                        item.groups().size(),
                        item.todaySessionsCount(),
                        new AdminTrainerListOutput.Load(
                                item.load().completed(),
                                item.load().planned(),
                                item.load().used(),
                                item.load().limit(),
                                item.load().percentage(),
                                item.load().status()
                        ),
                        new AdminTrainerListOutput.Reports(
                                item.reports().overdueCount(),
                                item.reports().pendingCount(),
                                item.reports().lastReportAt() == null ? null : item.reports().lastReportAt().toLocalDateTime()
                        )
                ))
                .toList();
    }

    private boolean matchesTrainerSearch(AdminTrainerListOutput.Item item, String search) {
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

    private boolean matchesGroupFilter(AdminTrainerListOutput.Item item, AdminTrainerFilterEnums.GroupFilter filter) {
        if (filter == null) {
            return true;
        }
        return switch (filter) {
            case WITHOUT_GROUP -> item.groupCount() == 0;
            case ONE_GROUP -> item.groupCount() == 1;
            case TWO_OR_THREE_GROUPS -> item.groupCount() >= 2 && item.groupCount() <= 3;
            case FOUR_OR_MORE_GROUPS -> item.groupCount() >= 4;
        };
    }

    private boolean matchesReportFilter(AdminTrainerListOutput.Item item, AdminTrainerFilterEnums.ReportStatus filter) {
        if (filter == null) {
            return true;
        }
        return switch (filter) {
            case NO_REPORTS -> item.reports().lastReportAt() == null
                               && item.reports().pendingCount() == 0
                               && item.reports().overdueCount() == 0;
            case PENDING -> item.reports().pendingCount() > 0;
            case OVERDUE -> item.reports().overdueCount() > 0;
            case SUBMITTED -> item.reports().lastReportAt() != null;
        };
    }

    private Comparator<AdminTrainerListOutput.Item> trainerComparator(
            AdminTrainerFilterEnums.SortField sort,
            org.springframework.data.domain.Sort.Direction direction
    ) {
        AdminTrainerFilterEnums.SortField resolvedSort = sort == null ? AdminTrainerFilterEnums.SortField.NAME : sort;
        boolean ascending = direction == null || direction.isAscending();
        Comparator<AdminTrainerListOutput.Item> comparator = switch (resolvedSort) {
            case NAME -> Comparator.comparing(AdminTrainerListOutput.Item::lastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(AdminTrainerListOutput.Item::firstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case GROUP_COUNT -> Comparator.comparingInt(AdminTrainerListOutput.Item::groupCount);
            case TODAY_SESSION_COUNT -> Comparator.comparingInt(AdminTrainerListOutput.Item::todaySessionsCount);
            case WORKLOAD -> Comparator.comparingInt(item -> item.load().percentage());
            case LAST_REPORT_AT -> Comparator.comparing(item -> item.reports().lastReportAt(), Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(AdminTrainerListOutput.Item::id);
    }

    private AdminTrainerOverviewOutput.NextSession toTrainerNextSession(
            CoachSessionAdminView session,
            Map<UUID, GroupDto> groupsById
    ) {
        GroupDto group = groupsById.get(session.groupId());
        return new AdminTrainerOverviewOutput.NextSession(
                session.sessionId(),
                session.groupId(),
                group == null ? "Unknown group" : group.name(),
                session.sessionDate(),
                session.scheduledStartAt().toLocalTime(),
                session.scheduledEndAt().toLocalTime(),
                session.status()
        );
    }

    private AdminTrainerOverviewOutput.LastReport toTrainerLastReport(
            CoachSessionAdminView session,
            Map<UUID, GroupDto> groupsById
    ) {
        GroupDto group = groupsById.get(session.groupId());
        return new AdminTrainerOverviewOutput.LastReport(
                session.sessionId(),
                session.groupId(),
                group == null ? "Unknown group" : group.name(),
                session.updatedAt()
        );
    }

    private List<AdminTrainerOverviewOutput.AttentionItem> buildAttentionItems(
            CoachDto coach,
            AdminCoachProfileOutput profile,
            AdminTrainerOverviewOutput.NextSession nextSession
    ) {
        List<AdminTrainerOverviewOutput.AttentionItem> items = new ArrayList<>();
        if (profile.groups().isEmpty()) {
            items.add(attention(
                    "NO_GROUPS",
                    "WARNING",
                    "Тренер не назначен в группы",
                    "Назначьте тренера в группу, чтобы появились расписание и занятия.",
                    coach.id(),
                    "OPEN_GROUP_ASSIGNMENT",
                    "Назначить группу"
            ));
            return items;
        }
        boolean hasScheduleIssue = profile.groups().stream().anyMatch(group -> group.weeklySlotsCount() == 0);
        if (hasScheduleIssue) {
            UUID groupId = profile.groups().stream()
                    .filter(group -> group.weeklySlotsCount() == 0)
                    .map(AdminCoachProfileOutput.GroupItem::groupId)
                    .findFirst()
                    .orElse(coach.id());
            items.add(attention(
                    "GROUP_SCHEDULE_NOT_CONFIGURED",
                    "WARNING",
                    "Расписание группы не настроено",
                    "У группы нет активных слотов, поэтому будущие занятия не создаются.",
                    groupId,
                    "OPEN_GROUP_SCHEDULE",
                    "Настроить расписание"
            ));
            return items;
        }
        if ("OVERLOADED".equals(profile.load().status())) {
            items.add(attention(
                    "OVERLOADED",
                    "CRITICAL",
                    "Тренер перегружен",
                    "Количество занятий превышает недельный лимит.",
                    coach.id(),
                    "OPEN_TRAINER_SCHEDULE",
                    "Открыть расписание"
            ));
        }
        if (profile.reports().overdueCount() > 0) {
            items.add(attention(
                    "OVERDUE_REPORTS",
                    "CRITICAL",
                    "Есть просроченные отчеты",
                    "Проверьте завершенные занятия без отчета.",
                    coach.id(),
                    "OPEN_TRAINER_REPORTS",
                    "Открыть отчеты"
            ));
        }
        if (nextSession == null && !"VACATION".equals(coach.workStatus() == null ? null : coach.workStatus().name())) {
            items.add(attention(
                    "NO_UPCOMING_SESSIONS",
                    "WARNING",
                    "Нет ближайших тренировок",
                    "Для тренера не найдено будущих активных занятий.",
                    coach.id(),
                    "OPEN_TRAINER_SCHEDULE",
                    "Открыть расписание"
            ));
        }
        if ("VACATION".equals(coach.workStatus() == null ? null : coach.workStatus().name())
                && coach.vacationTo() != null
                && !coach.vacationTo().isAfter(LocalDate.now().plusDays(7))) {
            items.add(attention(
                    "VACATION_ENDING",
                    "INFO",
                    "Отпуск скоро закончится",
                    "Проверьте рабочий статус тренера после окончания отпуска.",
                    coach.id(),
                    "OPEN_TRAINER_PROFILE",
                    "Открыть профиль"
            ));
        }
        return List.copyOf(items);
    }

    private AdminTrainerOverviewOutput.AttentionItem attention(
            String type,
            String severity,
            String title,
            String description,
            UUID entityId,
            String actionType,
            String actionLabel
    ) {
        return new AdminTrainerOverviewOutput.AttentionItem(
                type,
                severity,
                title,
                description,
                entityId,
                new AdminTrainerOverviewOutput.Action(actionType, actionLabel)
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
            case "NORMAL", "LOW" -> 0;
            case "MEDIUM" -> 1;
            case "HIGH" -> 2;
            case "FULL" -> 3;
            case "OVERLOADED" -> 4;
            default -> 3;
        };
    }

    private LocalDateTime coachCreatedAt(AdminCoachOverviewOutput.CoachItem item) {
        return item.createdAt();
    }

    private String resolveOverviewLoadStatus(int usedSlots, int maxSlots) {
        int percent = loadPercentage(usedSlots, maxSlots);
        if (percent > 100) {
            return "OVERLOADED";
        }
        if (percent == 100) {
            return "FULL";
        }
        if (percent >= 80) {
            return "HIGH";
        }
        if (percent >= 50) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveSessionReportStatus(CoachSessionAdminView session, LocalDateTime now) {
        if (session.reportDone()) {
            return "SUBMITTED";
        }
        if (session.scheduledEndAt() == null || session.scheduledEndAt().isAfter(now)) {
            return "NOT_REQUIRED";
        }
        LocalDateTime deadline = session.scheduledEndAt().plusHours(24);
        return now.isAfter(deadline) ? "OVERDUE" : "PENDING";
    }

    private int loadPercentage(int usedSlots, int maxSlots) {
        return maxSlots <= 0 ? 0 : (int) Math.round(usedSlots * 100.0 / maxSlots);
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
        List<CoachSessionAdminView> weekSessionViews = coachPort.getSessions(Set.of(coachId), groupIds, weekStart, weekEnd).stream()
                .filter(session -> !"CANCELLED".equals(session.status()))
                .toList();
        int completedSlots = (int) weekSessionViews.stream().filter(session -> "COMPLETED".equals(session.status())).count();
        int plannedSlots = weekSessionViews.size() - completedSlots;
        int usedSlots = weekSessionViews.size();
        String loadStatus = resolveOverviewLoadStatus(usedSlots, maxSlots);
        int loadPercent = loadPercentage(usedSlots, maxSlots);

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
                        item.changedBy(),
                        item.eventType(),
                        item.previousAccountStatus(),
                        item.newAccountStatus(),
                        item.previousWorkStatus(),
                        item.newWorkStatus(),
                        item.reason()
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
                coach.birthDate(),
                coach.phone(),
                coach.specialization(),
                coach.bio(),
                coach.active(),
                coach.accountStatus() == null ? null : coach.accountStatus().name(),
                coach.workStatus() == null ? null : coach.workStatus().name(),
                coach.vacationFrom(),
                coach.vacationTo(),
                coach.workStatusReason(),
                new AdminCoachProfileOutput.Load(
                        usedSlots,
                        maxSlots,
                        loadStatus,
                        completedSlots,
                        plannedSlots,
                        usedSlots,
                        maxSlots,
                        loadPercent
                ),
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

    private CoachStatus legacyStatus(AccountStatus accountStatus, WorkStatus workStatus) {
        if (workStatus != null) {
            return legacyStatus(workStatus);
        }
        return legacyStatus(accountStatus);
    }

    private void updateAccountStatus(UUID coachId, AccountStatus accountStatus) {
        switch (accountStatus) {
            case ACTIVE -> coachPort.enableCoach(coachId);
            case INACTIVE -> coachPort.disableCoach(coachId);
        }
    }

    private CoachStatusHistoryEventType resolveStatusHistoryEventType(
            boolean accountChanged,
            WorkStatus previousWorkStatus,
            WorkStatus newWorkStatus
    ) {
        boolean workChanged = newWorkStatus != null && newWorkStatus != previousWorkStatus;

        if (workChanged && newWorkStatus == WorkStatus.VACATION) {
            return CoachStatusHistoryEventType.VACATION_SET;
        }

        if (workChanged && previousWorkStatus == WorkStatus.VACATION) {
            return CoachStatusHistoryEventType.VACATION_ENDED;
        }

        if (accountChanged && workChanged) {
            return CoachStatusHistoryEventType.TRAINER_STATUS_CHANGED;
        }

        return accountChanged
                ? CoachStatusHistoryEventType.ACCOUNT_STATUS_CHANGED
                : CoachStatusHistoryEventType.WORK_STATUS_CHANGED;
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
