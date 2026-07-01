package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.branch.AdminBranchesOutput;
import kz.edu.soccerhub.common.dto.coach.AdminCoachProfileOutput;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.CoachSessionAdminView;
import kz.edu.soccerhub.common.dto.coach.CoachStatusHistoryDto;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.dispatcher.application.service.PasswordGenerator;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCoachServiceTest {

    @Mock
    private CoachPort coachPort;
    @Mock
    private AuthPort authPort;
    @Mock
    private AdminService adminService;
    @Mock
    private AdminBranchService adminBranchService;
    @Mock
    private PasswordGenerator passwordGenerator;
    @Mock
    private GroupSchedulePort groupSchedulePort;
    @Mock
    private GroupPort groupPort;
    @Mock
    private GroupCoachPort groupCoachPort;
    @Mock
    private ClientPort clientPort;

    private AdminCoachService service;

    @BeforeEach
    void setUp() {
        service = new AdminCoachService(
                coachPort,
                authPort,
                adminService,
                adminBranchService,
                passwordGenerator,
                groupSchedulePort,
                groupPort,
                groupCoachPort,
                clientPort
        );
    }

    @Test
    void shouldExtendCoachProfileGroupsReadModel() {
        UUID adminId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID firstGroupId = UUID.randomUUID();
        UUID secondGroupId = UUID.randomUUID();
        UUID firstGroupCoachId = UUID.randomUUID();
        UUID secondGroupCoachId = UUID.randomUUID();
        UUID firstSessionId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        when(adminBranchService.getAdminBranches(adminId)).thenReturn(List.of(
                AdminBranchesOutput.builder().branchId(branchId).name("Branch").build()
        ));
        when(coachPort.getCoach(coachId)).thenReturn(CoachDto.builder()
                .id(coachId)
                .firstName("Aibek")
                .lastName("Coach")
                .email("coach@example.com")
                .phone("+77010000000")
                .specialization("U10")
                .active(true)
                .build());
        when(coachPort.getBranchIds(coachId)).thenReturn(Set.of(branchId));
        when(groupCoachPort.getActiveAssignmentsByCoachId(coachId)).thenReturn(List.of(
                GroupCoachDto.builder()
                        .id(firstGroupCoachId)
                        .groupId(firstGroupId)
                        .coachId(coachId)
                        .role(CoachRole.MAIN)
                        .active(true)
                        .build(),
                GroupCoachDto.builder()
                        .id(secondGroupCoachId)
                        .groupId(secondGroupId)
                        .coachId(coachId)
                        .role(null)
                        .active(true)
                        .build()
        ));
        when(groupPort.getGroupsByIds(Set.of(firstGroupId, secondGroupId))).thenReturn(List.of(
                GroupDto.builder().groupId(firstGroupId).name("Falcons").branchId(branchId).status(GroupStatus.ACTIVE).build(),
                GroupDto.builder().groupId(secondGroupId).name("Wolves").branchId(branchId).status(GroupStatus.ACTIVE).build()
        ));
        when(groupSchedulePort.getActiveSchedulesByCoach(coachId)).thenReturn(List.of(
                GroupScheduleDto.builder()
                        .scheduleId(UUID.randomUUID())
                        .groupId(firstGroupId)
                        .coachId(coachId)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(18, 0))
                        .endTime(LocalTime.of(19, 0))
                        .startDate(today.minusDays(7))
                        .endDate(today.plusDays(30))
                        .status("ACTIVE")
                        .build(),
                GroupScheduleDto.builder()
                        .scheduleId(UUID.randomUUID())
                        .groupId(firstGroupId)
                        .coachId(coachId)
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .startTime(LocalTime.of(18, 0))
                        .endTime(LocalTime.of(19, 0))
                        .startDate(today.minusDays(7))
                        .endDate(today.plusDays(30))
                        .status("ACTIVE")
                        .build()
        ));
        when(coachPort.getSessions(
                Set.of(coachId),
                Set.of(firstGroupId, secondGroupId),
                today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        )).thenReturn(List.of(
                new CoachSessionAdminView(
                        firstSessionId,
                        coachId,
                        firstGroupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.plusDays(1),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(18, 0)),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(19, 0)),
                        "PLANNED",
                        false,
                        null
                )
        ));
        when(coachPort.getUpcomingSessions(coachId, today)).thenReturn(List.of(
                new CoachSessionAdminView(
                        UUID.randomUUID(),
                        coachId,
                        firstGroupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.plusDays(2),
                        LocalDateTime.of(today.plusDays(2), LocalTime.of(19, 0)),
                        LocalDateTime.of(today.plusDays(2), LocalTime.of(20, 0)),
                        "PLANNED",
                        false,
                        null
                ),
                new CoachSessionAdminView(
                        firstSessionId,
                        coachId,
                        firstGroupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.plusDays(1),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(18, 0)),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(19, 0)),
                        "CONFIRMED",
                        true,
                        null
                )
        ));
        when(clientPort.getGroupMembers(firstGroupId)).thenReturn(List.of(
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), "Player 1", LocalDate.of(2015, 1, 1), "ACTIVE", today.minusDays(10)),
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), "Player 2", LocalDate.of(2014, 1, 1), "CANCELLED", today.minusDays(20)),
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), "Player 3", LocalDate.of(2013, 1, 1), null, today.minusDays(30))
        ));
        when(clientPort.getGroupMembers(secondGroupId)).thenReturn(List.of());
        when(coachPort.getOverdueReportSessions(Set.of(coachId), Set.of(firstGroupId, secondGroupId), today)).thenReturn(List.of(
                new CoachSessionAdminView(
                        UUID.randomUUID(),
                        coachId,
                        secondGroupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.minusDays(1),
                        LocalDateTime.of(today.minusDays(1), LocalTime.of(18, 0)),
                        LocalDateTime.of(today.minusDays(1), LocalTime.of(19, 0)),
                        "PLANNED",
                        false,
                        null
                )
        ));
        when(coachPort.getReportedSessions(coachId)).thenReturn(List.of());
        when(coachPort.countOverdueReports(coachId, today)).thenReturn(1);
        when(coachPort.getStatusHistory(coachId)).thenReturn(List.of(
                new CoachStatusHistoryDto("ACTIVE", LocalDateTime.of(today, LocalTime.NOON), adminId)
        ));

        AdminCoachProfileOutput output = service.getCoachProfile(adminId, coachId);

        assertEquals(2, output.groups().size());

        AdminCoachProfileOutput.GroupItem firstGroup = output.groups().stream()
                .filter(group -> group.groupId().equals(firstGroupId))
                .findFirst()
                .orElseThrow();
        assertEquals("Falcons", firstGroup.groupName());
        assertEquals(firstGroupCoachId, firstGroup.groupCoachId());
        assertEquals("MAIN", firstGroup.role());
        assertEquals(3, firstGroup.studentsCount());
        assertEquals(2, firstGroup.activeStudentsCount());
        assertEquals(2, firstGroup.weeklySlotsCount());
        assertNotNull(firstGroup.nextSession());
        assertEquals(firstSessionId, firstGroup.nextSession().sessionId());
        assertEquals(today.plusDays(1), firstGroup.nextSession().sessionDate());
        assertEquals(LocalTime.of(18, 0), firstGroup.nextSession().startTime());
        assertEquals("CONFIRMED", firstGroup.nextSession().status());
        assertEquals(List.of(), firstGroup.riskFlags());

        AdminCoachProfileOutput.GroupItem secondGroup = output.groups().stream()
                .filter(group -> group.groupId().equals(secondGroupId))
                .findFirst()
                .orElseThrow();
        assertEquals("Wolves", secondGroup.groupName());
        assertEquals(secondGroupCoachId, secondGroup.groupCoachId());
        assertNull(secondGroup.role());
        assertEquals(0, secondGroup.studentsCount());
        assertEquals(0, secondGroup.activeStudentsCount());
        assertEquals(0, secondGroup.weeklySlotsCount());
        assertNull(secondGroup.nextSession());
        assertEquals(
                List.of("NO_STUDENTS", "NO_SCHEDULE", "NO_UPCOMING_SESSIONS", "OVERDUE_REPORTS"),
                secondGroup.riskFlags().stream().map(AdminCoachProfileOutput.RiskFlagItem::code).toList()
        );
    }
}
