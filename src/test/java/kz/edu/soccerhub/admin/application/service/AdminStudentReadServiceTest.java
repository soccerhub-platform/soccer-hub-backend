package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.student.AdminStudentDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentAttendanceOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentContractsOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentListItemOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentMembershipHistoryOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentRiskCode;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentsPageOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentsQuery;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceTimelineRecordDto;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRecordDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceSummaryDto;
import kz.edu.soccerhub.common.dto.contract.StudentContractSnapshotOutput;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.ContractPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupMembershipPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.organization.domain.model.GroupMembership;
import kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.common.port.MediaAccessPort;
import kz.edu.soccerhub.common.port.MediaAvatarPort;
import kz.edu.soccerhub.common.port.PaymentPort;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStudentReadServiceTest {

    @Mock
    private ClientPort clientPort;
    @Mock
    private ContractPort contractPort;
    @Mock
    private PaymentPort paymentPort;
    @Mock
    private CoachPort coachPort;
    @Mock
    private GroupSchedulePort groupSchedulePort;
    @Mock
    private GroupPort groupPort;
    @Mock
    private GroupMembershipPort groupMembershipPort;
    @Mock
    private AdminService adminService;
    @Mock
    private AdminBranchService adminBranchService;
    @Mock
    private MediaAvatarPort mediaAvatarPort;
    @Mock
    private MediaAccessPort mediaAccessPort;
    @Mock
    private TrainingSessionRepository trainingSessionRepository;
    private AdminStudentReadService service;

    @BeforeEach
    void setUp() {
        lenient().when(mediaAvatarPort.findActiveAvatars(ArgumentMatchers.any(), ArgumentMatchers.anyCollection()))
                .thenReturn(Map.of());
        lenient().when(trainingSessionRepository.findFirstByGroupIdAndStatusNotAndScheduledStartAtGreaterThanEqualOrderByScheduledStartAtAsc(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenReturn(Optional.empty());
        service = new AdminStudentReadService(
                clientPort,
                contractPort,
                paymentPort,
                coachPort,
                groupSchedulePort,
                groupPort,
                groupMembershipPort,
                adminService,
                adminBranchService,
                mediaAvatarPort,
                mediaAccessPort,
                trainingSessionRepository
        );
    }

    @Test
    void getStudentsShouldReturnAggregatedListFilteredByRisk() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID debtPlayerId = UUID.randomUUID();
        UUID noGroupPlayerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientPort.getStudentProfilesByBranch(branchId)).thenReturn(List.of(
                profile(branchId, debtPlayerId, "Alex Doe", "Jane Doe"),
                profile(branchId, noGroupPlayerId, "Mia Roe", "John Roe")
        ));
        when(contractPort.getStudentContracts(branchId, List.of(debtPlayerId, noGroupPlayerId))).thenReturn(List.of(
                contract(contractId, debtPlayerId, groupId, "Group A", ContractStatus.ACTIVE, LocalDate.now().plusDays(5))
        ));
        when(groupMembershipPort.findActiveByPlayerIdInAsOfDate(List.of(debtPlayerId, noGroupPlayerId), LocalDate.now())).thenReturn(List.of(
                GroupMembership.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .playerId(debtPlayerId)
                        .status(GroupMembershipStatus.ACTIVE)
                        .joinedAt(LocalDate.now().minusDays(10))
                        .build()
        ));
        when(groupPort.getGroupsByIds(Set.of(groupId))).thenReturn(List.of(
                groupDto(groupId, branchId, "Group A")
        ));
        when(coachPort.getAttendanceSummaries(Set.of(debtPlayerId, noGroupPlayerId))).thenReturn(List.of(
                new PlayerAttendanceSummaryDto(debtPlayerId, 82, 9, 1, 1, 0, 1),
                new PlayerAttendanceSummaryDto(noGroupPlayerId, 40, 2, 3, 0, 0, 3)
        ));
        when(mediaAvatarPort.findActiveAvatars(MediaOwnerType.PLAYER, List.of(debtPlayerId, noGroupPlayerId)))
                .thenReturn(Map.of());
        when(paymentPort.getContractPaymentSummaries(ArgumentMatchers.<List<ContractPaymentSummaryQueryInput>>any())).thenReturn(Map.of(
                contractId,
                new ContractPaymentSummaryOutput(
                        contractId,
                        BigDecimal.valueOf(50000),
                        BigDecimal.valueOf(20000),
                        BigDecimal.valueOf(30000),
                        BigDecimal.ZERO,
                        ContractPaymentStatus.PARTIALLY_PAID,
                        LocalDateTime.of(2026, 6, 23, 12, 0),
                        1
                )
        ));

        AdminStudentsPageOutput output = service.getStudents(
                adminId,
                new AdminStudentsQuery(branchId, null, null, null, AdminStudentRiskCode.DEBT, null),
                PageRequest.of(0, 20),
                "playerName,asc"
        );

        assertEquals(1, output.content().size());
        assertEquals(2, output.summary().total());
        assertEquals(0, output.summary().paid());
        assertEquals(1, output.summary().partiallyPaid());
        assertEquals(0, output.summary().unpaid());
        assertEquals(1, output.summary().withDebt());
        assertEquals(2, output.summary().withRisks());
        assertEquals(1, output.summary().withoutGroup());
        assertEquals(1, output.summary().lowAttendance());
        assertEquals(0, output.summary().expiredContracts());
        assertEquals(1, output.summary().endingSoon());
        assertEquals(debtPlayerId, output.content().getFirst().playerId());
        assertNotNull(output.content().getFirst().createdAt());
        assertEquals(ContractPaymentStatus.PARTIALLY_PAID, output.content().getFirst().paymentStatus());
        assertEquals("Group A", output.content().getFirst().groupName());
        assertTrue(output.content().getFirst().risks().stream().anyMatch(risk -> risk.code() == AdminStudentRiskCode.DEBT));
    }

    @Test
    void getStudentsShouldSupportCreatedAtSorting() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID olderPlayerId = UUID.randomUUID();
        UUID newerPlayerId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientPort.getStudentProfilesByBranch(branchId)).thenReturn(List.of(
                profile(branchId, olderPlayerId, "Older Student", "Parent One", LocalDateTime.of(2026, 7, 1, 10, 0)),
                profile(branchId, newerPlayerId, "Newer Student", "Parent Two", LocalDateTime.of(2026, 7, 9, 10, 0))
        ));
        when(contractPort.getStudentContracts(branchId, List.of(olderPlayerId, newerPlayerId))).thenReturn(List.of());
        when(groupMembershipPort.findActiveByPlayerIdInAsOfDate(List.of(olderPlayerId, newerPlayerId), LocalDate.now())).thenReturn(List.of());
        when(coachPort.getAttendanceSummaries(Set.of(olderPlayerId, newerPlayerId))).thenReturn(List.of());
        when(mediaAvatarPort.findActiveAvatars(MediaOwnerType.PLAYER, List.of(olderPlayerId, newerPlayerId))).thenReturn(Map.of());

        AdminStudentsPageOutput output = service.getStudents(
                adminId,
                new AdminStudentsQuery(branchId, null, null, null, null, null),
                PageRequest.of(0, 20),
                "createdAt,desc"
        );

        assertEquals(List.of(newerPlayerId, olderPlayerId), output.content().stream().map(AdminStudentListItemOutput::playerId).toList());
    }

    @Test
    void getStudentsShouldUseCurrentMembershipGroupInsteadOfLegacyContractGroup() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID legacyGroupId = UUID.randomUUID();
        UUID currentGroupId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientPort.getStudentProfilesByBranch(branchId)).thenReturn(List.of(
                profile(branchId, playerId, "Alex Doe", "Jane Doe")
        ));
        when(contractPort.getStudentContracts(branchId, List.of(playerId))).thenReturn(List.of(
                contract(contractId, playerId, legacyGroupId, "Legacy Group", ContractStatus.ACTIVE, LocalDate.now().plusDays(30))
        ));
        when(groupMembershipPort.findActiveByPlayerIdInAsOfDate(List.of(playerId), LocalDate.now())).thenReturn(List.of(
                GroupMembership.builder()
                        .id(UUID.randomUUID())
                        .groupId(currentGroupId)
                        .playerId(playerId)
                        .status(GroupMembershipStatus.ACTIVE)
                        .joinedAt(LocalDate.now().minusDays(2))
                        .build()
        ));
        when(groupPort.getGroupsByIds(Set.of(currentGroupId))).thenReturn(List.of(
                groupDto(currentGroupId, branchId, "Current Group")
        ));
        when(coachPort.getAttendanceSummaries(Set.of(playerId))).thenReturn(List.of());
        when(mediaAvatarPort.findActiveAvatars(MediaOwnerType.PLAYER, List.of(playerId))).thenReturn(Map.of());
        when(paymentPort.getContractPaymentSummaries(ArgumentMatchers.<List<ContractPaymentSummaryQueryInput>>any())).thenReturn(Map.of(
                contractId,
                new ContractPaymentSummaryOutput(
                        contractId,
                        BigDecimal.valueOf(50000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(50000),
                        BigDecimal.ZERO,
                        ContractPaymentStatus.UNPAID,
                        null,
                        0
                )
        ));

        AdminStudentsPageOutput output = service.getStudents(
                adminId,
                new AdminStudentsQuery(branchId, null, null, null, null, null),
                PageRequest.of(0, 20),
                "playerName,asc"
        );

        assertEquals(1, output.content().size());
        assertEquals(currentGroupId, output.content().getFirst().groupId());
        assertEquals("Current Group", output.content().getFirst().groupName());
        assertEquals(contractId, output.content().getFirst().contractId());
    }

    @Test
    void getStudentShouldPreferUpcomingContractAndPopulateDetailBlocks() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID upcomingContractId = UUID.randomUUID();
        UUID expiredContractId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientPort.getStudentProfile(playerId)).thenReturn(profile(branchId, playerId, "Alex Doe", "Jane Doe"));
        when(contractPort.getStudentContracts(branchId, playerId)).thenReturn(List.of(
                contract(expiredContractId, playerId, groupId, "Group A", ContractStatus.EXPIRED, LocalDate.now().minusDays(1)),
                contract(upcomingContractId, playerId, groupId, "Group A", ContractStatus.UPCOMING, LocalDate.now().plusDays(30))
        ));
        when(paymentPort.getContractPaymentSummaries(ArgumentMatchers.<List<ContractPaymentSummaryQueryInput>>any())).thenReturn(Map.of(
                upcomingContractId,
                new ContractPaymentSummaryOutput(
                        upcomingContractId,
                        BigDecimal.valueOf(60000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(60000),
                        BigDecimal.ZERO,
                        ContractPaymentStatus.UNPAID,
                        null,
                        0
                )
        ));
        when(coachPort.getAttendanceSummaries(Set.of(playerId))).thenReturn(List.of(
                new PlayerAttendanceSummaryDto(playerId, 75, 6, 2, 1, 0, 2)
        ));
        when(mediaAvatarPort.findActiveAvatar(MediaOwnerType.PLAYER, playerId)).thenReturn(Optional.empty());
        when(coachPort.getRecentAttendance(playerId, 10)).thenReturn(List.of(
                new PlayerAttendanceRecordDto(
                        playerId,
                        UUID.randomUUID(),
                        LocalDate.now().minusDays(1),
                        groupId,
                        "Group A",
                        TrainingSessionAttendanceStatus.PRESENT
                )
        ));
        when(paymentPort.getContractPayments(upcomingContractId)).thenReturn(List.of(
                new PaymentOutput(
                        UUID.randomUUID(),
                        upcomingContractId,
                        "CNT-2026-00001",
                        UUID.randomUUID(),
                        "Jane Doe",
                        playerId,
                        "Alex Doe",
                        branchId,
                        BigDecimal.valueOf(10000),
                        "KZT",
                        PaymentMethod.CASH,
                        PaymentStatus.PAID,
                        LocalDateTime.of(2026, 6, 23, 12, 0),
                        LocalDateTime.of(2026, 6, 23, 12, 0),
                        UUID.randomUUID(),
                        "Admin User",
                        "First payment",
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 6, 23, 12, 0),
                        LocalDateTime.of(2026, 6, 23, 12, 0)
                )
        ));
        when(groupSchedulePort.getActiveSchedulesByGroup(groupId)).thenReturn(List.of(
                GroupScheduleDto.builder()
                        .scheduleId(UUID.randomUUID())
                        .groupId(groupId)
                        .coachId(UUID.randomUUID())
                        .branchId(branchId)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(18, 0))
                        .endTime(LocalTime.of(19, 0))
                        .startDate(LocalDate.now().minusDays(3))
                        .endDate(LocalDate.now().plusMonths(1))
                        .scheduleType("REGULAR")
                        .status("ACTIVE")
                        .substitution(false)
                        .build()
        ));

        AdminStudentDetailsOutput output = service.getStudent(adminId, playerId);

        assertEquals(upcomingContractId, output.currentContract().id());
        assertEquals(ContractStatus.UPCOMING, output.currentContract().status());
        assertEquals("Group A", output.currentGroup().name());
        assertNotNull(output.currentGroup().scheduleLabel());
        assertEquals(1, output.recentPayments().size());
        assertEquals(1, output.recentAttendance().size());
        assertEquals(ContractPaymentStatus.UNPAID, output.currentContract().paymentStatus());
    }

    @Test
    void getMembershipHistoryShouldReturnMembershipTimeline() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID firstGroupId = UUID.randomUUID();
        UUID secondGroupId = UUID.randomUUID();
        UUID firstMembershipId = UUID.randomUUID();
        UUID secondMembershipId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientPort.getStudentProfile(playerId)).thenReturn(profile(branchId, playerId, "Alex Doe", "Jane Doe"));
        when(groupMembershipPort.findByPlayerIdOrderByJoinedAtDesc(playerId)).thenReturn(List.of(
                GroupMembership.builder()
                        .id(firstMembershipId)
                        .groupId(firstGroupId)
                        .playerId(playerId)
                        .status(GroupMembershipStatus.ACTIVE)
                        .joinedAt(LocalDate.of(2026, 8, 1))
                        .joinReason("TRANSFER")
                        .sourceContractId(UUID.randomUUID())
                        .build(),
                GroupMembership.builder()
                        .id(secondMembershipId)
                        .groupId(secondGroupId)
                        .playerId(playerId)
                        .status(GroupMembershipStatus.TRANSFERRED)
                        .joinedAt(LocalDate.of(2026, 5, 20))
                        .leftAt(LocalDate.of(2026, 7, 31))
                        .leaveReason("SCHEDULE_CHANGE")
                        .comment("Moved due to schedule")
                        .build()
        ));
        when(groupPort.getGroupsByIds(Set.of(firstGroupId, secondGroupId))).thenReturn(List.of(
                groupDto(firstGroupId, branchId, "Tangy Football"),
                groupDto(secondGroupId, branchId, "Adal")
        ));

        AdminStudentMembershipHistoryOutput output = service.getMembershipHistory(adminId, playerId);

        assertEquals(playerId, output.player().id());
        assertEquals("Alex Doe", output.player().fullName());
        assertEquals(2, output.items().size());
        assertEquals(firstMembershipId, output.items().getFirst().membershipId());
        assertEquals("Tangy Football", output.items().getFirst().group().name());
        assertEquals("ACTIVE", output.items().getFirst().status());
        assertTrue(output.items().getFirst().capabilities().canTransfer());
        assertFalse(output.items().getFirst().capabilities().canRemove());
        assertFalse(output.items().get(1).capabilities().canTransfer());
        assertFalse(output.items().get(1).capabilities().canRemove());
        assertEquals("SCHEDULE_CHANGE", output.items().get(1).leaveReason());
    }

    @Test
    void getAttendanceShouldIncludeUnmarkedTimelineRecordsAndKeepSummaryBeforeStatusFilter() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID presentSessionId = UUID.randomUUID();
        UUID unmarkedSessionId = UUID.randomUUID();
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        GroupMembership membership = GroupMembership.builder()
                .id(membershipId)
                .groupId(groupId)
                .playerId(playerId)
                .status(GroupMembershipStatus.ACTIVE)
                .joinedAt(from)
                .build();
        PlayerAttendanceTimelineRecordDto presentRecord = attendanceRecord(
                presentSessionId,
                groupId,
                LocalDate.now().minusDays(5),
                TrainingSessionAttendanceStatus.PRESENT
        );
        PlayerAttendanceTimelineRecordDto unmarkedRecord = attendanceRecord(
                unmarkedSessionId,
                groupId,
                LocalDate.now().minusDays(3),
                null
        );

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientPort.getStudentProfile(playerId)).thenReturn(profile(branchId, playerId, "Alex Doe", "Jane Doe"));
        when(groupMembershipPort.findByPlayerIdOrderByJoinedAtDesc(playerId)).thenReturn(List.of(membership));
        when(coachPort.getAttendanceTimeline(playerId, Set.of(groupId), from, to))
                .thenReturn(List.of(unmarkedRecord, presentRecord));
        when(groupPort.getGroupsByIds(Set.of(groupId))).thenReturn(List.of(groupDto(groupId, branchId, "Group A")));

        AdminStudentAttendanceOutput output = service.getAttendance(adminId, playerId, from, to, null, null);

        assertEquals(2, output.summary().sessionsCount());
        assertEquals(1, output.summary().markedCount());
        assertEquals(1, output.summary().presentCount());
        assertEquals(1, output.summary().unmarkedCount());
        assertEquals(100, output.summary().attendanceRate());
        assertEquals("UNMARKED", output.items().getFirst().attendanceStatus());

        AdminStudentAttendanceOutput filtered = service.getAttendance(adminId, playerId, from, to, null, "UNMARKED");
        assertEquals(2, filtered.summary().sessionsCount());
        assertEquals(1, filtered.items().size());
        assertEquals(unmarkedSessionId, filtered.items().getFirst().sessionId());
    }

    @Test
    void getContractsShouldReturnHistoryWithPaymentSummaryAndCurrentMarker() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID activeContractId = UUID.randomUUID();
        UUID expiredContractId = UUID.randomUUID();
        StudentContractSnapshotOutput active = contract(
                activeContractId,
                playerId,
                groupId,
                "Group A",
                ContractStatus.ACTIVE,
                LocalDate.now().plusDays(10)
        );
        StudentContractSnapshotOutput expired = contract(
                expiredContractId,
                playerId,
                groupId,
                "Group A",
                ContractStatus.EXPIRED,
                LocalDate.now().minusDays(20)
        );

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientPort.getStudentProfile(playerId)).thenReturn(profile(branchId, playerId, "Alex Doe", "Jane Doe"));
        when(contractPort.getStudentContracts(branchId, playerId)).thenReturn(List.of(expired, active));
        when(paymentPort.getContractPaymentSummaries(ArgumentMatchers.<List<ContractPaymentSummaryQueryInput>>any()))
                .thenReturn(Map.of(
                        activeContractId,
                        new ContractPaymentSummaryOutput(
                                activeContractId,
                                BigDecimal.valueOf(50000),
                                BigDecimal.valueOf(20000),
                                BigDecimal.valueOf(30000),
                                BigDecimal.ZERO,
                                ContractPaymentStatus.PARTIALLY_PAID,
                                LocalDateTime.now().minusDays(2),
                                1
                        ),
                        expiredContractId,
                        new ContractPaymentSummaryOutput(
                                expiredContractId,
                                BigDecimal.valueOf(50000),
                                BigDecimal.valueOf(50000),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                ContractPaymentStatus.PAID,
                                LocalDateTime.now().minusMonths(2),
                                2
                        )
                ));

        AdminStudentContractsOutput output = service.getContracts(adminId, playerId);

        assertEquals(2, output.summary().totalCount());
        assertEquals(1, output.summary().activeCount());
        assertEquals(1, output.summary().endingSoonCount());
        assertEquals(1, output.summary().withDebtCount());
        assertEquals(activeContractId, output.items().getFirst().id());
        assertTrue(output.items().getFirst().current());
        assertEquals(BigDecimal.valueOf(30000), output.items().getFirst().outstandingAmount());
        assertFalse(output.items().get(1).current());
    }

    @Test
    void getStudentShouldUseActiveMembershipForCurrentGroupWhenContractStillPointsToOldGroup() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID oldGroupId = UUID.randomUUID();
        UUID newGroupId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientPort.getStudentProfile(playerId)).thenReturn(profile(branchId, playerId, "Alex Doe", "Jane Doe"));
        when(contractPort.getStudentContracts(branchId, playerId)).thenReturn(List.of(
                contract(contractId, playerId, oldGroupId, "Old Group", ContractStatus.ACTIVE, LocalDate.now().plusDays(30))
        ));
        when(groupMembershipPort.findByPlayerIdOrderByJoinedAtDesc(playerId)).thenReturn(List.of(
                GroupMembership.builder()
                        .id(UUID.randomUUID())
                        .groupId(newGroupId)
                        .playerId(playerId)
                        .status(GroupMembershipStatus.ACTIVE)
                        .joinedAt(LocalDate.now().minusDays(1))
                        .build()
        ));
        when(groupPort.getGroupById(newGroupId)).thenReturn(groupDto(newGroupId, branchId, "New Group"));
        when(paymentPort.getContractPaymentSummaries(ArgumentMatchers.<List<ContractPaymentSummaryQueryInput>>any())).thenReturn(Map.of(
                contractId,
                new ContractPaymentSummaryOutput(
                        contractId,
                        BigDecimal.valueOf(50000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(50000),
                        BigDecimal.ZERO,
                        ContractPaymentStatus.UNPAID,
                        null,
                        0
                )
        ));
        when(coachPort.getAttendanceSummaries(Set.of(playerId))).thenReturn(List.of());
        when(mediaAvatarPort.findActiveAvatar(MediaOwnerType.PLAYER, playerId)).thenReturn(Optional.empty());
        when(coachPort.getRecentAttendance(playerId, 10)).thenReturn(List.of());
        when(paymentPort.getContractPayments(contractId)).thenReturn(List.of());
        when(groupSchedulePort.getActiveSchedulesByGroup(newGroupId)).thenReturn(List.of());

        AdminStudentDetailsOutput output = service.getStudent(adminId, playerId);

        assertEquals(newGroupId, output.currentGroup().id());
        assertEquals("New Group", output.currentGroup().name());
        assertEquals(contractId, output.currentContract().id());
    }

    private AdminDto admin(UUID adminId) {
        return AdminDto.builder()
                .id(adminId)
                .firstName("Admin")
                .lastName("User")
                .branchesId(Set.of())
                .build();
    }

    private StudentProfileDto profile(UUID branchId, UUID playerId, String playerName, String parentName) {
        return profile(branchId, playerId, playerName, parentName, LocalDateTime.of(2026, 7, 1, 9, 0));
    }

    private StudentProfileDto profile(
            UUID branchId,
            UUID playerId,
            String playerName,
            String parentName,
            LocalDateTime createdAt
    ) {
        return new StudentProfileDto(
                branchId,
                playerId,
                playerName,
                playerName,
                "",
                null,
                createdAt,
                LocalDate.of(2015, 5, 10),
                UUID.randomUUID(),
                parentName,
                "+77010000000",
                "parent@example.com",
                "ACTIVE"
        );
    }

    private StudentContractSnapshotOutput contract(
            UUID contractId,
            UUID playerId,
            UUID groupId,
            String groupName,
            ContractStatus status,
            LocalDate endDate
    ) {
        LocalDate startDate = status == ContractStatus.UPCOMING ? LocalDate.now().plusDays(1) : LocalDate.now().minusDays(20);
        return new StudentContractSnapshotOutput(
                contractId,
                playerId,
                UUID.randomUUID(),
                "CNT-2026-00001",
                status,
                startDate,
                endDate,
                BigDecimal.valueOf(50000),
                "KZT",
                groupId,
                groupName,
                UUID.randomUUID(),
                "Coach Smith"
        );
    }

    private kz.edu.soccerhub.common.dto.group.GroupDto groupDto(UUID groupId, UUID branchId, String name) {
        return kz.edu.soccerhub.common.dto.group.GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name(name)
                .build();
    }

    private PlayerAttendanceTimelineRecordDto attendanceRecord(
            UUID sessionId,
            UUID groupId,
            LocalDate date,
            TrainingSessionAttendanceStatus status
    ) {
        return new PlayerAttendanceTimelineRecordDto(
                sessionId,
                groupId,
                date,
                date.atTime(18, 0),
                date.atTime(19, 0),
                TrainingSessionStatus.COMPLETED,
                TrainingSessionStatus.COMPLETED.name(),
                status,
                null
        );
    }
}
