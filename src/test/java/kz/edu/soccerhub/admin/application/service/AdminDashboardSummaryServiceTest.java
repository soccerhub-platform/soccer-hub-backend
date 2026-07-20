package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardSummaryResponse;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.analytics.DashboardLeadAnalyticsOutput;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.CoachSessionAdminView;
import kz.edu.soccerhub.common.dto.contract.StudentContractSnapshotOutput;
import kz.edu.soccerhub.common.dto.coach.SessionAttendanceSummaryDto;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentSearchQuery;
import kz.edu.soccerhub.common.dto.payment.PaymentsPageOutput;
import kz.edu.soccerhub.common.port.AnalyticsPort;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.ContractPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.common.port.PaymentPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardSummaryServiceTest {

    @Mock
    private AdminService adminService;
    @Mock
    private AdminBranchService adminBranchService;
    @Mock
    private BranchPort branchPort;
    @Mock
    private GroupPort groupPort;
    @Mock
    private GroupCoachPort groupCoachPort;
    @Mock
    private GroupSchedulePort groupSchedulePort;
    @Mock
    private CoachPort coachPort;
    @Mock
    private ClientPort clientPort;
    @Mock
    private ContractPort contractPort;
    @Mock
    private PaymentPort paymentPort;
    @Mock
    private AnalyticsPort analyticsPort;

    private AdminDashboardSummaryService service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardSummaryService(
                adminService,
                adminBranchService,
                branchPort,
                groupPort,
                groupCoachPort,
                groupSchedulePort,
                coachPort,
                clientPort,
                contractPort,
                paymentPort,
                analyticsPort
        );
    }

    @Test
    void shouldBuildDashboardSummaryFromAggregates() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 24);

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(branchPort.findById(branchId)).thenReturn(Optional.of(
                BranchDto.builder().id(branchId).name("Главный филиал").timezone("Asia/Almaty").build()
        ));
        when(groupPort.getGroupsByBranch(branchId)).thenReturn(List.of(
                GroupDto.builder().groupId(groupId).name("Adal").branchId(branchId).status(GroupStatus.ACTIVE).build()
        ));
        when(clientPort.getStudentProfilesByBranch(branchId)).thenReturn(List.of(
                new kz.edu.soccerhub.common.dto.student.StudentProfileDto(
                        branchId,
                        playerId,
                        "Player",
                        "Player",
                        "",
                        null,
                        LocalDateTime.of(2026, 6, 1, 10, 0),
                        LocalDate.of(2015, 5, 10),
                        UUID.randomUUID(),
                        "Parent",
                        "+77010000000",
                        "parent@example.com",
                        "ACTIVE"
                )
        ));
        when(contractPort.getStudentContracts(branchId, List.of(playerId))).thenReturn(List.of(
                new StudentContractSnapshotOutput(
                        contractId,
                        playerId,
                        branchId,
                        "CNT-1",
                        kz.edu.soccerhub.client.domain.enums.ContractStatus.ACTIVE,
                        date.minusDays(20),
                        date.plusDays(3),
                        BigDecimal.valueOf(50000),
                        "KZT",
                        groupId,
                        "Adal",
                        coachId,
                        "Арсен Рахметулы"
                )
        ));
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(
                GroupCoachDto.builder().groupId(groupId).coachId(coachId).role(CoachRole.MAIN).active(true).build()
        ));
        when(groupSchedulePort.getActiveSchedulesByGroup(groupId, date)).thenReturn(List.of(
                kz.edu.soccerhub.common.dto.group.GroupScheduleDto.builder().groupId(groupId).build()
        ));
        when(coachPort.getCoaches(Set.of(coachId))).thenReturn(List.of(
                CoachDto.builder().id(coachId).firstName("Арсен").lastName("Рахметулы").active(true).build()
        ));
        when(coachPort.getSessions(Set.of(coachId), Set.of(groupId), date, date)).thenReturn(List.of(
                new CoachSessionAdminView(
                        sessionId,
                        coachId,
                        groupId,
                        UUID.randomUUID(),
                        "TEMPORARY",
                        date,
                        LocalDateTime.of(2026, 6, 24, 20, 0),
                        LocalDateTime.of(2026, 6, 24, 21, 0),
                        "PLANNED",
                        false,
                        LocalDateTime.of(2026, 6, 24, 10, 0)
                )
        ));
        when(coachPort.getSessions(
                Set.of(coachId),
                Set.of(groupId),
                LocalDate.of(2026, 6, 22),
                LocalDate.of(2026, 6, 28)
        )).thenReturn(List.of(
                new CoachSessionAdminView(
                        sessionId,
                        coachId,
                        groupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        date,
                        LocalDateTime.of(2026, 6, 24, 20, 0),
                        LocalDateTime.of(2026, 6, 24, 21, 0),
                        "PLANNED",
                        false,
                        LocalDateTime.of(2026, 6, 24, 10, 0)
                )
        ));
        when(coachPort.getOverdueReportSessions(Set.of(coachId), Set.of(groupId), date)).thenReturn(List.of());
        when(coachPort.getSessionAttendanceSummaries(Set.of(sessionId))).thenReturn(List.of(
                new SessionAttendanceSummaryDto(sessionId, 10, 8)
        ));
        when(clientPort.countStudentsAsOf(branchId, date, "Asia/Almaty")).thenReturn(158L);
        when(clientPort.countStudentsAsOf(branchId, date.minusDays(1), "Asia/Almaty")).thenReturn(152L);
        when(clientPort.countCreatedStudents(branchId, date, "Asia/Almaty")).thenReturn(5L);
        when(clientPort.countCreatedStudents(branchId, date.minusDays(1), "Asia/Almaty")).thenReturn(3L);
        when(paymentPort.listPayments(ArgumentMatchers.any(PaymentSearchQuery.class), ArgumentMatchers.eq(org.springframework.data.domain.Pageable.unpaged())))
                .thenReturn(new PaymentsPageOutput(
                        List.of(new PaymentOutput(
                                paymentId,
                                UUID.randomUUID(),
                                "CNT-1",
                                UUID.randomUUID(),
                                "Parent",
                                UUID.randomUUID(),
                                "Player",
                                branchId,
                                BigDecimal.valueOf(20000),
                                "KZT",
                                PaymentMethod.CASH,
                                PaymentStatus.PAID,
                                LocalDateTime.of(2026, 6, 24, 12, 0),
                                LocalDateTime.of(2026, 6, 24, 12, 0),
                                adminId,
                                "Admin",
                                null,
                                null,
                                null,
                                null,
                                LocalDateTime.of(2026, 6, 24, 12, 0),
                                LocalDateTime.of(2026, 6, 24, 12, 0)
                        )),
                        1,
                        1,
                        0,
                        1
                ));
        when(analyticsPort.getDashboardLeadAnalytics(branchId, date, "Asia/Almaty")).thenReturn(
                new DashboardLeadAnalyticsOutput(
                        3,
                        45,
                        1,
                        Map.of(
                                LeadStatus.NEW, 3L,
                                LeadStatus.CONTACTED, 2L,
                                LeadStatus.QUALIFIED, 1L,
                                LeadStatus.TRIAL_SCHEDULED, 1L,
                                LeadStatus.WON, 1L,
                                LeadStatus.LOST, 0L
                        ),
                        List.of()
                )
        );
        when(analyticsPort.countCreatedLeads(branchId, date, "Asia/Almaty")).thenReturn(3L);
        when(analyticsPort.countCreatedLeads(branchId, date.minusDays(1), "Asia/Almaty")).thenReturn(2L);
        when(coachPort.getSessions(Set.of(coachId), Set.of(groupId), date.minusDays(1), date.minusDays(1))).thenReturn(List.of(
                new CoachSessionAdminView(
                        UUID.randomUUID(),
                        coachId,
                        groupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        date.minusDays(1),
                        LocalDateTime.of(2026, 6, 23, 20, 0),
                        LocalDateTime.of(2026, 6, 23, 21, 0),
                        "PLANNED",
                        false,
                        LocalDateTime.of(2026, 6, 23, 10, 0)
                )
        ));
        when(paymentPort.listPayments(
                ArgumentMatchers.argThat(query -> query.paidFrom() != null && query.paidFrom().toLocalDate().equals(date.minusDays(1))),
                ArgumentMatchers.eq(org.springframework.data.domain.Pageable.unpaged())
        )).thenReturn(new PaymentsPageOutput(
                List.of(new PaymentOutput(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "CNT-0",
                        UUID.randomUUID(),
                        "Parent",
                        UUID.randomUUID(),
                        "Player",
                        branchId,
                        BigDecimal.valueOf(10000),
                        "KZT",
                        PaymentMethod.CASH,
                        PaymentStatus.PAID,
                        LocalDateTime.of(2026, 6, 23, 12, 0),
                        LocalDateTime.of(2026, 6, 23, 12, 0),
                        adminId,
                        "Admin",
                        null,
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 6, 23, 12, 0),
                        LocalDateTime.of(2026, 6, 23, 12, 0)
                )),
                1,
                1,
                0,
                1
        ));

        AdminDashboardSummaryResponse response = service.getSummary(adminId, branchId, date, "Asia/Almaty", false);

        assertEquals("Главный филиал", response.meta().branchName());
        assertEquals(4, response.kpis().items().size());
        assertEquals("newLeads", response.kpis().items().get(0).code());
        assertEquals(3, response.kpis().items().get(0).value());
        assertEquals("paymentsToday", response.kpis().items().get(3).code());
        assertEquals(1, response.kpis().items().get(3).value());
        assertEquals(BigDecimal.valueOf(20000), response.kpis().items().get(3).amount());
        assertEquals(158L, response.branchSummary().studentsTotal());
        assertEquals(6L, response.branchSummary().studentsDelta());
        assertEquals(1L, response.branchSummary().trainingsVisited());
        assertEquals(1L, response.branchSummary().trainingsTotal());
        assertEquals(100, response.branchSummary().attendancePercent());
        assertEquals(45, response.branchSummary().avgFirstResponseMinutes());
        assertEquals("contracts-ending-soon", response.risks().items().getFirst().code());
        assertEquals(1, response.todaySchedule().summary().total());
        assertEquals("Adal", response.todaySchedule().items().getFirst().groupName());
        assertEquals("TEMPORARY", response.todaySchedule().items().getFirst().scheduleType());
        assertTrue(response.alerts().attention().stream().anyMatch(item -> "waiting-leads".equals(item.id())));
        assertTrue(response.alerts().attention().stream().anyMatch(item -> "contracts-ending-soon".equals(item.id())));
        assertEquals(2, response.alerts().topCards().size());
        assertEquals(date.minusDays(6), response.weeklyDynamics().period().from());
        assertEquals(date, response.weeklyDynamics().period().to());
        assertEquals(3, response.weeklyDynamics().series().size());
    }
}
