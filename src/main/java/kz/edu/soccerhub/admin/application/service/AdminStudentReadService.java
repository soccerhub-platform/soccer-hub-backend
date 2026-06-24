package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.student.*;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRecordDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceSummaryDto;
import kz.edu.soccerhub.common.dto.contract.StudentContractSnapshotOutput;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.*;
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
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    @Transactional(readOnly = true)
    public AdminStudentsPageOutput getStudents(UUID adminId, AdminStudentsQuery query, Pageable pageable, String sort) {
        verifyAdminAccessToBranch(adminId, query.branchId());

        List<StudentProfileDto> profiles = clientPort.getStudentProfilesByBranch(query.branchId());
        Map<UUID, List<StudentContractSnapshotOutput>> contractsByPlayerId = groupContractsByPlayer(
                contractPort.getStudentContracts(query.branchId(), profiles.stream().map(StudentProfileDto::playerId).toList())
        );
        Map<UUID, PlayerAttendanceSummaryDto> attendanceByPlayerId = coachPort.getAttendanceSummaries(
                        profiles.stream().map(StudentProfileDto::playerId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(PlayerAttendanceSummaryDto::playerId, item -> item));

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

        List<AdminStudentListItemOutput> items = profiles.stream()
                .map(profile -> toListItem(
                        profile,
                        currentContractsByPlayerId.get(profile.playerId()),
                        paymentSummaries,
                        attendanceByPlayerId.get(profile.playerId())
                ))
                .filter(item -> matches(item, query))
                .sorted(resolveComparator(sort))
                .toList();

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int fromIndex = Math.min(pageNumber * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());

        return new AdminStudentsPageOutput(
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
        List<AdminStudentRiskOutput> risks = buildRisks(currentContract, paymentSummary, attendanceSummary);

        return new AdminStudentDetailsOutput(
                new AdminStudentDetailsOutput.PlayerBlock(
                        profile.playerId(),
                        profile.playerFullName(),
                        profile.birthDate(),
                        calculateAge(profile.birthDate())
                ),
                new AdminStudentDetailsOutput.ClientBlock(
                        profile.clientId(),
                        profile.clientFullName(),
                        profile.phone(),
                        profile.email(),
                        profile.clientStatus()
                ),
                toCurrentGroupBlock(currentContract),
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

    private AdminStudentListItemOutput toListItem(
            StudentProfileDto profile,
            StudentContractSnapshotOutput currentContract,
            Map<UUID, ContractPaymentSummaryOutput> paymentSummaries,
            PlayerAttendanceSummaryDto attendanceSummary
    ) {
        ContractPaymentSummaryOutput paymentSummary = currentContract == null ? null : paymentSummaries.get(currentContract.id());
        PlayerAttendanceSummaryDto resolvedAttendance = attendanceSummary == null
                ? new PlayerAttendanceSummaryDto(profile.playerId(), 0, 0, 0, 0, 0, 0)
                : attendanceSummary;
        List<AdminStudentRiskOutput> risks = buildRisks(currentContract, paymentSummary, resolvedAttendance);

        return new AdminStudentListItemOutput(
                profile.playerId(),
                profile.playerFullName(),
                profile.birthDate(),
                calculateAge(profile.birthDate()),
                profile.clientId(),
                profile.clientFullName(),
                profile.phone(),
                profile.email(),
                currentContract == null ? null : currentContract.groupId(),
                currentContract == null ? null : currentContract.groupName(),
                currentContract == null ? null : currentContract.coachName(),
                currentContract == null ? null : currentContract.id(),
                currentContract == null ? null : currentContract.contractNumber(),
                currentContract == null ? null : currentContract.status(),
                currentContract == null ? null : currentContract.endDate(),
                paymentSummary == null ? null : paymentSummary.paymentStatus(),
                paymentSummary == null ? BigDecimal.ZERO : paymentSummary.paidAmount(),
                paymentSummary == null ? BigDecimal.ZERO : paymentSummary.outstandingAmount(),
                resolvedAttendance.attendanceRate(),
                resolvedAttendance.missedLast30Days(),
                risks
        );
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
            case "contractEndDate" -> Comparator.comparing(AdminStudentListItemOutput::contractEndDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "outstandingAmount" -> Comparator.comparing(AdminStudentListItemOutput::outstandingAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "attendanceRate" -> Comparator.comparing(AdminStudentListItemOutput::attendanceRate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "parentName" -> Comparator.comparing(AdminStudentListItemOutput::parentName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(AdminStudentListItemOutput::playerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
        return ascending ? comparator : comparator.reversed();
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
            StudentContractSnapshotOutput currentContract,
            ContractPaymentSummaryOutput paymentSummary,
            PlayerAttendanceSummaryDto attendanceSummary
    ) {
        Map<AdminStudentRiskCode, AdminStudentRiskOutput> risks = new LinkedHashMap<>();

        if (currentContract == null || currentContract.groupId() == null) {
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

    private int totalAttendanceCount(PlayerAttendanceSummaryDto summary) {
        return summary.presentCount() + summary.absentCount() + summary.lateCount() + summary.excusedCount();
    }

    private AdminStudentDetailsOutput.CurrentGroupBlock toCurrentGroupBlock(StudentContractSnapshotOutput currentContract) {
        if (currentContract == null || currentContract.groupId() == null) {
            return null;
        }

        List<GroupScheduleDto> schedules = groupSchedulePort.getActiveSchedulesByGroup(currentContract.groupId());
        return new AdminStudentDetailsOutput.CurrentGroupBlock(
                currentContract.groupId(),
                currentContract.groupName(),
                currentContract.coachName(),
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
