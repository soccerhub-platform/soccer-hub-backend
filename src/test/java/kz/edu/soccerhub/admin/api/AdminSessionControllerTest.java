package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.dto.session.AdminCancelSessionInput;
import kz.edu.soccerhub.admin.application.dto.session.AdminGroupSessionsOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminRescheduleSessionInput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSessionDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSubstituteCoachInput;
import kz.edu.soccerhub.admin.application.service.AdminSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSessionControllerTest {

    private final AdminSessionService adminSessionService = Mockito.mock(AdminSessionService.class);

    private AdminSessionController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminSessionController(adminSessionService);
    }

    @Test
    void shouldReturnGroupSessions() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminGroupSessionsOutput output = new AdminGroupSessionsOutput(
                groupId,
                from,
                to,
                List.of(new AdminGroupSessionsOutput.SessionItem(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        LocalDate.of(2026, 7, 12),
                        LocalDateTime.of(2026, 7, 12, 20, 0),
                        LocalDateTime.of(2026, 7, 12, 21, 0),
                        "PLANNED",
                        "PLANNED",
                        null,
                        new AdminGroupSessionsOutput.LocationRef(UUID.randomUUID(), "Поле №2"),
                        List.of(new AdminGroupSessionsOutput.CoachRef(coachId, "Арсен Рахметулы", "MAIN")),
                        20,
                        new AdminGroupSessionsOutput.AttendanceSummary(20, 0, 0),
                        new AdminGroupSessionsOutput.Capabilities(true, true, true, true)
                ))
        );

        when(adminSessionService.getGroupSessions(eq(adminId), eq(groupId), eq(from), eq(to), eq("PLANNED"), eq(coachId)))
                .thenReturn(output);

        assertSame(output, controller.getGroupSessions(jwt, groupId, from, to, "PLANNED", coachId).getBody());
        verify(adminSessionService).getGroupSessions(eq(adminId), eq(groupId), eq(from), eq(to), eq("PLANNED"), eq(coachId));
    }

    @Test
    void shouldReturnSessionDetails() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminSessionDetailsOutput output = new AdminSessionDetailsOutput(
                sessionId,
                new AdminSessionDetailsOutput.GroupRef(groupId, "Adal"),
                UUID.randomUUID(),
                LocalDate.of(2026, 7, 12),
                LocalDateTime.of(2026, 7, 12, 20, 0),
                LocalDateTime.of(2026, 7, 12, 21, 0),
                null,
                null,
                "PLANNED",
                "PLANNED",
                null,
                new AdminGroupSessionsOutput.LocationRef(UUID.randomUUID(), "Поле №2"),
                List.of(),
                20,
                new AdminGroupSessionsOutput.AttendanceSummary(20, 0, 0),
                new AdminGroupSessionsOutput.Capabilities(true, true, true, true)
        );

        when(adminSessionService.getSessionDetails(eq(adminId), eq(sessionId))).thenReturn(output);

        assertSame(output, controller.getSessionDetails(jwt, sessionId).getBody());
        verify(adminSessionService).getSessionDetails(eq(adminId), eq(sessionId));
    }

    @Test
    void shouldForwardCancelCommand() {
        UUID adminId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminCancelSessionInput input = new AdminCancelSessionInput("COACH_UNAVAILABLE", "Заболел");
        AdminSessionDetailsOutput output = new AdminSessionDetailsOutput(
                sessionId,
                new AdminSessionDetailsOutput.GroupRef(UUID.randomUUID(), "Adal"),
                null,
                LocalDate.of(2026, 7, 12),
                LocalDateTime.of(2026, 7, 12, 20, 0),
                LocalDateTime.of(2026, 7, 12, 21, 0),
                null,
                null,
                "CANCELLED",
                "CANCELLED",
                "COACH_UNAVAILABLE: Заболел",
                null,
                List.of(),
                20,
                new AdminGroupSessionsOutput.AttendanceSummary(20, 0, 0),
                new AdminGroupSessionsOutput.Capabilities(false, false, false, false)
        );
        when(adminSessionService.cancelSession(eq(adminId), eq(sessionId), eq(input))).thenReturn(output);

        assertSame(output, controller.cancelSession(jwt, sessionId, input).getBody());
        verify(adminSessionService).cancelSession(eq(adminId), eq(sessionId), eq(input));
    }

    @Test
    void shouldForwardRescheduleCommand() {
        UUID adminId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminRescheduleSessionInput input = new AdminRescheduleSessionInput(
                LocalDateTime.of(2026, 7, 13, 20, 0),
                LocalDateTime.of(2026, 7, 13, 21, 0),
                UUID.randomUUID(),
                "Поле занято"
        );
        AdminSessionDetailsOutput output = new AdminSessionDetailsOutput(
                sessionId,
                new AdminSessionDetailsOutput.GroupRef(UUID.randomUUID(), "Adal"),
                null,
                LocalDate.of(2026, 7, 13),
                input.startsAt(),
                input.endsAt(),
                null,
                null,
                "PLANNED",
                "PLANNED",
                null,
                new AdminGroupSessionsOutput.LocationRef(input.locationId(), "Поле №3"),
                List.of(),
                20,
                new AdminGroupSessionsOutput.AttendanceSummary(20, 0, 0),
                new AdminGroupSessionsOutput.Capabilities(true, true, true, false)
        );
        when(adminSessionService.rescheduleSession(eq(adminId), eq(sessionId), eq(input))).thenReturn(output);

        assertSame(output, controller.rescheduleSession(jwt, sessionId, input).getBody());
        verify(adminSessionService).rescheduleSession(eq(adminId), eq(sessionId), eq(input));
    }

    @Test
    void shouldForwardSubstituteCoachCommand() {
        UUID adminId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminSubstituteCoachInput input = new AdminSubstituteCoachInput(UUID.randomUUID(), UUID.randomUUID(), "Основной отсутствует");
        AdminSessionDetailsOutput output = new AdminSessionDetailsOutput(
                sessionId,
                new AdminSessionDetailsOutput.GroupRef(UUID.randomUUID(), "Adal"),
                null,
                LocalDate.of(2026, 7, 12),
                LocalDateTime.of(2026, 7, 12, 20, 0),
                LocalDateTime.of(2026, 7, 12, 21, 0),
                null,
                null,
                "PLANNED",
                "PLANNED",
                null,
                null,
                List.of(new AdminGroupSessionsOutput.CoachRef(input.substituteCoachId(), "Арсен Гизатов", "ASSISTANT")),
                20,
                new AdminGroupSessionsOutput.AttendanceSummary(20, 0, 0),
                new AdminGroupSessionsOutput.Capabilities(true, true, true, false)
        );
        when(adminSessionService.substituteCoach(eq(adminId), eq(sessionId), eq(input))).thenReturn(output);

        assertSame(output, controller.substituteCoach(jwt, sessionId, input).getBody());
        verify(adminSessionService).substituteCoach(eq(adminId), eq(sessionId), eq(input));
    }
}
