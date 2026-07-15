package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.student.*;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRecordDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceSummaryDto;
import kz.edu.soccerhub.common.dto.contract.StudentContractSnapshotOutput;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import kz.edu.soccerhub.organization.domain.model.GroupMembership;
import kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStudentReadService {

    private static final int LOW_ATTENDANCE_THRESHOLD = 60;
    private static final int ENDING_SOON_DAYS = 7;
    private static final int RECENT_PAYMENTS_LIMIT = 10;
    private static final int RECENT_ATTENDANCE_LIMIT = 10;

    private final ClientPort clientPort;
    private final ContractPort contractPort;
    private final PaymentPort paymentPort;
    private final CoachPort coachPort;
    private final GroupSchedulePort groupSchedulePort;
    private final GroupPort groupPort;
    private final GroupMembershipPort groupMembershipPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final MediaAvatarPort mediaAvatarPort;
    private final MediaAccessPort mediaAccessPort;

    @Transactional(readOnly = true)
    public AdminStudentsPageOutput getStudents(UUID adminId, AdminStudentsQuery query, Pageable pageable, String sort) {
        verifyAdminAccessToBranch(adminId, query.branchId());

        List<StudentProfileDto> profiles = clientPort.getStudentProfilesByBranch(query.branchId());
        List<UUID> playerIds = profiles.stream().map(StudentProfileDto::playerId).toList();
        Map<UUID, List<StudentContractSnapshotOutput>> contractsByPlayerId = groupContractsByPlayer(
                contractPort.getStudentContracts(query.branchId(), playerIds)
        );
        Map<UUID, GroupMembership> currentMembershipsByPlayerId = groupMembershipPort.findActiveByPlayerIdInAsOfDate(
                        playerIds,
                        LocalDate.now()
                ).stream()
                .collect(Collectors.groupingBy(GroupMembership::getPlayerId))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> selectCurrentMembership(entry.getValue())));
        Map<UUID, String> currentGroupNamesById = resolveGroupNames(
                currentMembershipsByPlayerId.values().stream()
                        .filter(Objects::nonNull)
                        .map(GroupMembership::getGroupId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        Map<UUID, MediaAsset> currentGroupAvatarsById = mediaAvatarPort.findActiveAvatars(
                MediaOwnerType.GROUP,
                currentMembershipsByPlayerId.values().stream()
                        .filter(Objects::nonNull)
                        .map(GroupMembership::getGroupId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        if (currentGroupAvatarsById == null) {
            currentGroupAvatarsById = Map.of();
        }
        Map<UUID, MediaAsset> currentGroupAvatars = currentGroupAvatarsById;
        Map<UUID, PlayerAttendanceSummaryDto> attendanceByPlayerId = coachPort.getAttendanceSummaries(
                        new HashSet<>(playerIds)
                ).stream()
                .collect(Collectors.toMap(PlayerAttendanceSummaryDto::playerId, item -> item));
        Map<UUID, MediaAsset> avatarsByPlayerId = mediaAvatarPort.findActiveAvatars(
                MediaOwnerType.PLAYER,
                playerIds
        );
        if (avatarsByPlayerId == null) {
            avatarsByPlayerId = Map.of();
        }
        Map<UUID, MediaAsset> playerAvatars = avatarsByPlayerId;

        Map<UUID, StudentContractSnapshotOutput> currentContractsByPlayerId = new HashMap<>();
        List<ContractPaymentSummaryQueryInput> paymentQueries = new ArrayList<>();
        for (StudentProfileDto profile : profiles) {
            StudentContractSnapshotOutput current = selectCurrentContract(contractsByPlayerId.get(profile.playerId()));
            currentContractsByPlayerId.put(profile.playerId(), current);
            if (current != null) {
                paymentQueries.add(new ContractPaymentSummaryQueryInput(current.id(), current.amount()));
            }
        }
        Map<UUID, ContractPaymentSummaryOutput> paymentSummaries = paymentQueries.isEmpty()
                ? Map.of()
                : paymentPort.getContractPaymentSummaries(paymentQueries);

        List<AdminStudentListItemOutput> allItems = profiles.stream()
                .map(profile -> toListItem(
                        profile,
                        currentMembershipsByPlayerId.get(profile.playerId()),
                        currentGroupNamesById,
                        currentGroupAvatars,
                        currentContractsByPlayerId.get(profile.playerId()),
                        paymentSummaries,
                        attendanceByPlayerId.get(profile.playerId()),
                        toMediaAssetResponse(playerAvatars.get(profile.playerId()))
                ))
                .toList();

        AdminStudentsPageOutput.Summary summary = buildSummary(allItems);

        List<AdminStudentListItemOutput> items = allItems.stream()
                .filter(item -> matches(item, query))
                .sorted(resolveComparator(sort))
                .toList();

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int fromIndex = Math.min(pageNumber * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());

        return new AdminStudentsPageOutput(
                summary,
                items.subList(fromIndex, toIndex),
                items.size(),
                pageSize == 0 ? 0 : (int) Math.ceil(items.size() / (double) pageSize),
                pageNumber,
                pageSize
        );
    }

    @Transactional(readOnly = true)
    public AdminStudentDetailsOutput getStudent(UUID adminId, UUID playerId) {
        StudentProfileDto profile = clientPort.getStudentProfile(playerId);
        verifyAdminAccessToBranch(adminId, profile.branchId());

        List<StudentContractSnapshotOutput> contracts = contractPort.getStudentContracts(profile.branchId(), playerId);
        List<GroupMembership> memberships = groupMembershipPort.findByPlayerIdOrderByJoinedAtDesc(playerId);
        GroupMembership currentMembership = selectCurrentMembership(memberships);
        StudentContractSnapshotOutput currentContract = selectCurrentContract(contracts);
        ContractPaymentSummaryOutput paymentSummary = currentContract == null
                ? null
                : paymentPort.getContractPaymentSummaries(List.of(
                        new ContractPaymentSummaryQueryInput(currentContract.id(), currentContract.amount())
                )).get(currentContract.id());

        PlayerAttendanceSummaryDto attendanceSummary = coachPort.getAttendanceSummaries(Set.of(playerId)).stream()
                .findFirst()
                .orElse(new PlayerAttendanceSummaryDto(playerId, 0, 0, 0, 0, 0, 0));
        List<PlayerAttendanceRecordDto> recentAttendance = coachPort.getRecentAttendance(playerId, RECENT_ATTENDANCE_LIMIT);
        List<PaymentOutput> recentPayments = currentContract == null
                ? List.of()
                : paymentPort.getContractPayments(currentContract.id()).stream().limit(RECENT_PAYMENTS_LIMIT).toList();
        List<AdminStudentRiskOutput> risks = buildRisks(
                currentMembership == null ? null : currentMembership.getGroupId(),
                currentContract,
                paymentSummary,
                attendanceSummary
        );
        MediaAssetResponse avatar = toMediaAssetResponse(mediaAvatarPort.findActiveAvatar(
                MediaOwnerType.PLAYER,
                playerId
        ).orElse(null));

        return new AdminStudentDetailsOutput(
                new AdminStudentDetailsOutput.PlayerBlock(
                        profile.playerId(),
                        profile.playerFullName(),
                        profile.birthDate(),
                        calculateAge(profile.birthDate()),
                        avatar
                ),
                new AdminStudentDetailsOutput.ClientBlock(
                        profile.clientId(),
                        profile.clientFullName(),
                        profile.phone(),
                        profile.email(),
                        profile.clientStatus()
                ),
                toCurrentGroupBlock(currentMembership, currentContract),
                toCurrentContractBlock(currentContract, paymentSummary),
                new AdminStudentDetailsOutput.AttendanceSummaryBlock(
                        attendanceSummary.attendanceRate(),
                        attendanceSummary.presentCount(),
                        attendanceSummary.absentCount(),
                        attendanceSummary.lateCount(),
                        attendanceSummary.missedLast30Days()
                ),
                recentPayments.stream()
                        .map(item -> new AdminStudentDetailsOutput.RecentPaymentBlock(
                                item.id(),
                                item.amount(),
                                item.currency(),
                                item.method(),
                                item.status(),
                                item.paidAt(),
                                item.comment()
                        ))
                        .toList(),
                recentAttendance.stream()
                        .map(item -> new AdminStudentDetailsOutput.RecentAttendanceBlock(
                                item.sessionId(),
                                item.sessionDate(),
                                item.groupName(),
                                item.status()
                        ))
                        .toList(),
                risks
        );
    }

    @Transactional(readOnly = true)
    public AdminStudentMembershipHistoryOutput getMembershipHistory(UUID adminId, UUID playerId) {
        StudentProfileDto profile = clientPort.getStudentProfile(playerId);
        verifyAdminAccessToBranch(adminId, profile.branchId());

        List<GroupMembership> memberships = groupMembershipPort.findByPlayerIdOrderByJoinedAtDesc(playerId);
        Set<UUID> groupIds = memberships.stream()
                .map(GroupMembership::getGroupId)
                .collect(Collectors.toSet());
        Map<UUID, String> groupNamesById = groupIds.isEmpty()
                ? Map.of()
                : groupPort.getGroupsByIds(groupIds).stream()
                .collect(Collectors.toMap(kz.edu.soccerhub.common.dto.group.GroupDto::groupId,
                        kz.edu.soccerhub.common.dto.group.GroupDto::name));
        Map<UUID, MediaAsset> groupAvatarsById = mediaAvatarPort.findActiveAvatars(MediaOwnerType.GROUP, groupIds);
        if (groupAvatarsById == null) {
            groupAvatarsById = Map.of();
        }
        Map<UUID, MediaAsset> groupAvatars = groupAvatarsById;

        return new AdminStudentMembershipHistoryOutput(
                new AdminStudentMembershipHistoryOutput.Player(profile.playerId(), profile.playerFullName()),
                memberships.stream()
                        .map(item -> new AdminStudentMembershipHistoryOutput.Item(
                                item.getId(),
                                new AdminStudentMembershipHistoryOutput.Group(
                                        item.getGroupId(),
                                        groupNamesById.get(item.getGroupId()),
                                        toMediaAssetResponse(groupAvatars.get(item.getGroupId()))
                                ),
                                item.getStatus().name(),
                                item.getJoinedAt(),
                                item.getLeftAt(),
                                item.getJoinReason(),
                                item.getLeaveReason(),
                                item.getComment(),
                                item.getSourceContractId()
                        ))
                        .toList()
        );
    }

    private AdminStudentListItemOutput toListItem(
            StudentProfileDto profile,
            GroupMembership currentMembership,
            Map<UUID, String> currentGroupNamesById,
            Map<UUID, MediaAsset> currentGroupAvatarsById,
            StudentContractSnapshotOutput currentContract,
            Map<UUID, ContractPaymentSummaryOutput> paymentSummaries,
            PlayerAttendanceSummaryDto attendanceSummary,
            MediaAssetResponse avatar
    ) {
        ContractPaymentSummaryOutput paymentSummary = currentContract == null ? null : paymentSummaries.get(currentContract.id());
        PlayerAttendanceSummaryDto resolvedAttendance = attendanceSummary == null
                ? new PlayerAttendanceSummaryDto(profile.playerId(), 0, 0, 0, 0, 0, 0)
                : attendanceSummary;
        UUID currentGroupId = currentMembership == null ? null : currentMembership.getGroupId();
        String currentGroupName = currentMembership == null ? null : currentGroupNamesById.get(currentMembership.getGroupId());
        MediaAssetResponse currentGroupAvatar = currentMembership == null
                ? null
                : toMediaAssetResponse(currentGroupAvatarsById.get(currentMembership.getGroupId()));
        String currentCoachName = currentContract != null && Objects.equals(currentContract.groupId(), currentGroupId)
                ? currentContract.coachName()
                : null;
        List<AdminStudentRiskOutput> risks = buildRisks(currentGroupId, currentContract, paymentSummary, resolvedAttendance);

        return new AdminStudentListItemOutput(
                profile.playerId(),
                profile.playerFullName(),
                profile.createdAt(),
                profile.birthDate(),
                calculateAge(profile.birthDate()),
                profile.clientId(),
                profile.clientFullName(),
                profile.phone(),
                profile.email(),
                currentGroupId,
                currentGroupName,
                currentGroupAvatar,
                currentCoachName,
                currentContract == null ? null : currentContract.id(),
                currentContract == null ? null : currentContract.contractNumber(),
                currentContract == null ? null : currentContract.status(),
                currentContract == null ? null : currentContract.endDate(),
                paymentSummary == null ? null : paymentSummary.paymentStatus(),
                paymentSummary == null ? BigDecimal.ZERO : paymentSummary.paidAmount(),
                paymentSummary == null ? BigDecimal.ZERO : paymentSummary.outstandingAmount(),
                resolvedAttendance.attendanceRate(),
                resolvedAttendance.missedLast30Days(),
                avatar,
                risks
        );
    }

    private MediaAssetResponse toMediaAssetResponse(MediaAsset asset) {
        return asset == null ? null : mediaAccessPort.toResponse(asset);
    }

    private AdminStudentsPageOutput.Summary buildSummary(List<AdminStudentListItemOutput> items) {
        int total = items.size();
        int paid = 0;
        int partiallyPaid = 0;
        int unpaid = 0;
        int withDebt = 0;
        int withRisks = 0;
        int withoutGroup = 0;
        int lowAttendance = 0;
        int expiredContracts = 0;
        int endingSoon = 0;

        for (AdminStudentListItemOutput item : items) {
            if (item.paymentStatus() == ContractPaymentStatus.PAID) {
                paid++;
            } else if (item.paymentStatus() == ContractPaymentStatus.PARTIALLY_PAID) {
                partiallyPaid++;
            } else if (item.paymentStatus() == ContractPaymentStatus.UNPAID) {
                unpaid++;
            }

            if (item.outstandingAmount() != null && item.outstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
                withDebt++;
            }

            if (!item.risks().isEmpty()) {
                withRisks++;
            }

            if (item.groupId() == null) {
                withoutGroup++;
            }

            if (hasRisk(item, AdminStudentRiskCode.LOW_ATTENDANCE)) {
                lowAttendance++;
            }
            if (hasRisk(item, AdminStudentRiskCode.EXPIRED_CONTRACT)) {
                expiredContracts++;
            }
            if (hasRisk(item, AdminStudentRiskCode.ENDING_SOON)) {
                endingSoon++;
            }
        }

        return new AdminStudentsPageOutput.Summary(
                total,
                paid,
                partiallyPaid,
                unpaid,
                withDebt,
                withRisks,
                withoutGroup,
                lowAttendance,
                expiredContracts,
                endingSoon
        );
    }

    private boolean hasRisk(
            AdminStudentListItemOutput item,
            AdminStudentRiskCode riskCode
    ) {
        return item.risks().stream().anyMatch(risk -> risk.code() == riskCode);
    }

    private boolean matches(AdminStudentListItemOutput item, AdminStudentsQuery query) {
        if (query.paymentStatus() != null && query.paymentStatus() != item.paymentStatus()) {
            return false;
        }
        if (query.contractStatus() != null && query.contractStatus() != item.contractStatus()) {
            return false;
        }
        if (query.groupId() != null && !Objects.equals(query.groupId(), item.groupId())) {
            return false;
        }
        if (query.risk() != null && item.risks().stream().noneMatch(risk -> risk.code() == query.risk())) {
            return false;
        }
        return matchesSearch(item, query.search());
    }

    private boolean matchesSearch(AdminStudentListItemOutput item, String search) {
        String normalized = trimToNull(search);
        if (normalized == null) {
            return true;
        }
        String needle = normalized.toLowerCase(Locale.ROOT);
        return contains(item.playerName(), needle)
               || contains(item.parentName(), needle)
               || contains(item.phone(), needle)
               || contains(item.email(), needle)
               || contains(item.groupName(), needle)
               || contains(item.contractNumber(), needle);
    }

    private Comparator<AdminStudentListItemOutput> resolveComparator(String sort) {
        String normalized = trimToNull(sort);
        String field = "playerName";
        boolean ascending = true;

        if (normalized != null) {
            String[] parts = normalized.split(",", 2);
            field = parts[0].trim();
            if (parts.length > 1) {
                ascending = !"desc".equalsIgnoreCase(parts[1].trim());
            }
        }

        Comparator<AdminStudentListItemOutput> comparator = switch (field) {
            case "createdAt" -> Comparator.comparing(AdminStudentListItemOutput::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "contractEndDate" -> Comparator.comparing(AdminStudentListItemOutput::contractEndDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "outstandingAmount" -> Comparator.comparing(AdminStudentListItemOutput::outstandingAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "paidAmount" -> Comparator.comparing(AdminStudentListItemOutput::paidAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "attendanceRate" -> Comparator.comparing(AdminStudentListItemOutput::attendanceRate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "playerName" -> Comparator.comparing(AdminStudentListItemOutput::playerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "parentName" -> Comparator.comparing(AdminStudentListItemOutput::parentName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(AdminStudentListItemOutput::playerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
        if (!Set.of("playerName", "parentName", "contractEndDate", "outstandingAmount", "paidAmount", "attendanceRate", "createdAt").contains(field)) {
            throw new BadRequestException("Unknown student sort field", field);
        }

        Comparator<AdminStudentListItemOutput> stableComparator = ascending ? comparator : comparator.reversed();
        return stableComparator.thenComparing(AdminStudentListItemOutput::playerId);
    }

    private Map<UUID, List<StudentContractSnapshotOutput>> groupContractsByPlayer(List<StudentContractSnapshotOutput> contracts) {
        return contracts.stream().collect(Collectors.groupingBy(StudentContractSnapshotOutput::playerId));
    }

    private StudentContractSnapshotOutput selectCurrentContract(List<StudentContractSnapshotOutput> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return null;
        }

        List<StudentContractSnapshotOutput> active = contracts.stream()
                .filter(contract -> contract.status() == ContractStatus.ACTIVE)
                .sorted(Comparator.comparing(StudentContractSnapshotOutput::endDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (!active.isEmpty()) {
            return active.getFirst();
        }

        List<StudentContractSnapshotOutput> upcoming = contracts.stream()
                .filter(contract -> contract.status() == ContractStatus.UPCOMING)
                .sorted(Comparator.comparing(StudentContractSnapshotOutput::startDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (!upcoming.isEmpty()) {
            return upcoming.getFirst();
        }

        return contracts.stream()
                .filter(contract -> contract.status() == ContractStatus.EXPIRED)
                .max(Comparator.comparing(StudentContractSnapshotOutput::endDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private List<AdminStudentRiskOutput> buildRisks(
            UUID currentGroupId,
            StudentContractSnapshotOutput currentContract,
            ContractPaymentSummaryOutput paymentSummary,
            PlayerAttendanceSummaryDto attendanceSummary
    ) {
        Map<AdminStudentRiskCode, AdminStudentRiskOutput> risks = new LinkedHashMap<>();

        if (currentGroupId == null) {
            risks.put(AdminStudentRiskCode.NO_GROUP, new AdminStudentRiskOutput(
                    AdminStudentRiskCode.NO_GROUP,
                    "No group assigned",
                    AdminStudentRiskSeverity.WARNING
            ));
        }

        if (currentContract != null && currentContract.status() == ContractStatus.EXPIRED) {
            risks.put(AdminStudentRiskCode.EXPIRED_CONTRACT, new AdminStudentRiskOutput(
                    AdminStudentRiskCode.EXPIRED_CONTRACT,
                    "Contract expired",
                    AdminStudentRiskSeverity.CRITICAL
            ));
        }

        if (currentContract != null
                && currentContract.endDate() != null
                && currentContract.status() == ContractStatus.ACTIVE
                && !currentContract.endDate().isBefore(LocalDate.now())
                && !currentContract.endDate().isAfter(LocalDate.now().plusDays(ENDING_SOON_DAYS))) {
            risks.put(AdminStudentRiskCode.ENDING_SOON, new AdminStudentRiskOutput(
                    AdminStudentRiskCode.ENDING_SOON,
                    "Contract ends soon",
                    AdminStudentRiskSeverity.WARNING
            ));
        }

        if (paymentSummary != null && paymentSummary.outstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
            risks.put(AdminStudentRiskCode.DEBT, new AdminStudentRiskOutput(
                    AdminStudentRiskCode.DEBT,
                    "Outstanding payment",
                    AdminStudentRiskSeverity.CRITICAL
            ));
        }

        if (attendanceSummary != null
                && totalAttendanceCount(attendanceSummary) > 0
                && attendanceSummary.attendanceRate() < LOW_ATTENDANCE_THRESHOLD) {
            risks.put(AdminStudentRiskCode.LOW_ATTENDANCE, new AdminStudentRiskOutput(
                    AdminStudentRiskCode.LOW_ATTENDANCE,
                    "Low attendance",
                    AdminStudentRiskSeverity.WARNING
            ));
        }

        return List.copyOf(risks.values());
    }

    private Map<UUID, String> resolveGroupNames(Set<UUID> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }

        return groupPort.getGroupsByIds(groupIds).stream()
                .collect(Collectors.toMap(GroupDto::groupId, GroupDto::name));
    }

    private GroupMembership selectCurrentMembership(List<GroupMembership> memberships) {
        if (memberships == null || memberships.isEmpty()) {
            return null;
        }

        return memberships.stream()
                .filter(item -> item.getStatus() == GroupMembershipStatus.ACTIVE || item.getStatus() == GroupMembershipStatus.UPCOMING)
                .sorted(Comparator.comparing(
                                (GroupMembership item) -> item.getStatus() == GroupMembershipStatus.ACTIVE ? 0 : 1
                        ).thenComparing(GroupMembership::getJoinedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private int totalAttendanceCount(PlayerAttendanceSummaryDto summary) {
        return summary.presentCount() + summary.absentCount() + summary.lateCount() + summary.excusedCount();
    }

    private AdminStudentDetailsOutput.CurrentGroupBlock toCurrentGroupBlock(
            GroupMembership currentMembership,
            StudentContractSnapshotOutput currentContract
    ) {
        UUID groupId = null;
        String groupName = null;
        String coachName = null;

        if (currentMembership != null && currentMembership.getGroupId() != null) {
            GroupDto group = groupPort.getGroupById(currentMembership.getGroupId());
            groupId = group.groupId();
            groupName = group.name();
            if (currentContract != null && Objects.equals(currentContract.groupId(), groupId)) {
                coachName = currentContract.coachName();
            }
        } else if (currentContract != null && currentContract.groupId() != null) {
            groupId = currentContract.groupId();
            groupName = currentContract.groupName();
            coachName = currentContract.coachName();
        }

        if (groupId == null) {
            return null;
        }

        List<GroupScheduleDto> schedules = groupSchedulePort.getActiveSchedulesByGroup(groupId);
        return new AdminStudentDetailsOutput.CurrentGroupBlock(
                groupId,
                groupName,
                getGroupAvatar(groupId),
                coachName,
                buildScheduleLabel(schedules),
                resolveNextSessionAt(schedules)
        );
    }

    private AdminStudentDetailsOutput.CurrentContractBlock toCurrentContractBlock(
            StudentContractSnapshotOutput currentContract,
            ContractPaymentSummaryOutput paymentSummary
    ) {
        if (currentContract == null || paymentSummary == null) {
            return null;
        }
        return new AdminStudentDetailsOutput.CurrentContractBlock(
                currentContract.id(),
                currentContract.contractNumber(),
                currentContract.status(),
                currentContract.startDate(),
                currentContract.endDate(),
                currentContract.amount(),
                currentContract.currency(),
                paymentSummary.paymentStatus(),
                paymentSummary.paidAmount(),
                paymentSummary.outstandingAmount(),
                paymentSummary.overpaidAmount(),
                paymentSummary.lastPaidAt()
        );
    }

    private String buildScheduleLabel(List<GroupScheduleDto> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }
        return schedules.stream()
                .sorted(Comparator.comparing(GroupScheduleDto::dayOfWeek).thenComparing(GroupScheduleDto::startTime))
                .map(slot -> shortDay(slot.dayOfWeek()) + " " + formatTime(slot.startTime()) + "-" + formatTime(slot.endTime()))
                .collect(Collectors.joining(", "));
    }

    private LocalDateTime resolveNextSessionAt(List<GroupScheduleDto> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        return schedules.stream()
                .map(slot -> nextOccurrence(slot, today, now))
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime nextOccurrence(GroupScheduleDto slot, LocalDate today, LocalDateTime now) {
        if (slot.dayOfWeek() == null || slot.startTime() == null) {
            return null;
        }

        int daysAhead = (slot.dayOfWeek().getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        LocalDate candidateDate = today.plusDays(daysAhead);
        LocalDate startDate = slot.startDate();
        LocalDate endDate = slot.endDate();

        if (startDate != null && candidateDate.isBefore(startDate)) {
            candidateDate = startDate;
        }
        while (candidateDate.getDayOfWeek() != slot.dayOfWeek()) {
            candidateDate = candidateDate.plusDays(1);
        }
        if (endDate != null && candidateDate.isAfter(endDate)) {
            return null;
        }

        LocalDateTime candidate = LocalDateTime.of(candidateDate, slot.startTime());
        if (!candidate.isAfter(now)) {
            candidateDate = candidateDate.plusWeeks(1);
            if (endDate != null && candidateDate.isAfter(endDate)) {
                return null;
            }
            candidate = LocalDateTime.of(candidateDate, slot.startTime());
        }
        return candidate;
    }

    private String shortDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Mon";
            case TUESDAY -> "Tue";
            case WEDNESDAY -> "Wed";
            case THURSDAY -> "Thu";
            case FRIDAY -> "Fri";
            case SATURDAY -> "Sat";
            case SUNDAY -> "Sun";
        };
    }

    private String formatTime(LocalTime time) {
        return time == null ? "" : time.toString();
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private MediaAssetResponse getGroupAvatar(UUID groupId) {
        Optional<MediaAsset> avatar = mediaAvatarPort.findActiveAvatar(MediaOwnerType.GROUP, groupId);
        return toMediaAssetResponse(avatar == null ? null : avatar.orElse(null));
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void verifyAdminAccessToBranch(UUID adminId, UUID branchId) {
        verifyAdminExists(adminId);
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }

    private void verifyAdminExists(UUID adminId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
    }
}
