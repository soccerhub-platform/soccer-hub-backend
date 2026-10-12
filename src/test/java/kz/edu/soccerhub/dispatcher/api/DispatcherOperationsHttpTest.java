package kz.edu.soccerhub.dispatcher.api;

import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.EmployeeAccessDto;
import kz.edu.soccerhub.common.dto.company.FollowUpCommand;
import kz.edu.soccerhub.common.dto.company.TrialMutationCommand;
import kz.edu.soccerhub.common.dto.lead.LeadRoutingCommand;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.port.EmployeeAccessPort;
import kz.edu.soccerhub.dispatcher.application.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DispatcherOperationsHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean DispatcherOperationsService operations;
    @MockitoBean DispatcherLeadWorkflowService leads;
    @MockitoBean DispatcherLeadService leadCreation;
    @MockitoBean EmployeeAccessPort access;
    private final UUID user = UUID.randomUUID();
    private final UUID record = UUID.randomUUID();
    private final UUID branch = UUID.randomUUID();

    @BeforeEach void allowDispatcher() {
        when(access.getAccess(user)).thenReturn(new EmployeeAccessDto(true, Set.of(Role.DISPATCHER), Set.of()));
    }

    private RequestPostProcessor token(String role) {
        return jwt().jwt(j -> j.subject(user.toString()).claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority(role));
    }

    @Test void followUpPreservesOffsetDeadlineAndVersion() throws Exception {
        mvc.perform(post("/dispatcher/operations/contracts/{id}/follow-up", record)
                .with(token("DISPATCHER")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"operation":"CONTACT","version":3,"nextAction":"Проверить оплату",
                     "dueAt":"2026-10-12T10:30:00+05:00","outcome":"PAYMENT_PROMISED",
                     "promisedPaymentDate":"2026-10-13","comment":"Связались с родителем"}
                    """))
                .andExpect(status().isOk());
        var command = ArgumentCaptor.forClass(FollowUpCommand.class);
        verify(operations).updateFollowUp(eq(user), eq(record), command.capture());
        assertThat(command.getValue().version()).isEqualTo(3L);
        assertThat(command.getValue().dueAt()).isEqualTo(Instant.parse("2026-10-12T05:30:00Z"));
        assertThat(command.getValue().promisedPaymentDate().toString()).isEqualTo("2026-10-13");
    }

    @Test void trialPreservesBusinessLocalTimeAndVersion() throws Exception {
        mvc.perform(post("/dispatcher/operations/trials/{id}", record)
                .with(token("DISPATCHER")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"operation":"RESULT","version":2,"nextActionAt":"2026-10-12T10:30:00","comment":"Перезвонить"}
                    """))
                .andExpect(status().isOk());
        var command = ArgumentCaptor.forClass(TrialMutationCommand.class);
        verify(operations).updateTrial(eq(user), eq(record), command.capture());
        assertThat(command.getValue().version()).isEqualTo(2L);
        assertThat(command.getValue().nextActionAt()).isEqualTo(LocalDateTime.of(2026, 10, 12, 10, 30));
    }

    @Test void routingPassesAuthenticatedActorAndReason() throws Exception {
        mvc.perform(post("/dispatcher/leads/{id}/routing", record)
                .with(token("DISPATCHER")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"branchId\":\"" + branch + "\",\"version\":4,\"reason\":\"Ближе к дому\"}"))
                .andExpect(status().isOk());
        var command = ArgumentCaptor.forClass(LeadRoutingCommand.class);
        verify(leads).route(eq(user), eq(record), command.capture());
        assertThat(command.getValue().branchId()).isEqualTo(branch);
        assertThat(command.getValue().reason()).isEqualTo("Ближе к дому");
        assertThat(command.getValue().version()).isEqualTo(4L);
    }

    @Test void missingVersionsAndRoutingReasonReturnValidationErrors() throws Exception {
        mvc.perform(post("/dispatcher/operations/trials/{id}", record)
                .with(token("DISPATCHER")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\":\"ATTENDANCE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.version").exists());
        mvc.perform(post("/dispatcher/operations/contracts/{id}/follow-up", record)
                .with(token("DISPATCHER")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\":\"PLAN\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.version").exists());
        mvc.perform(post("/dispatcher/leads/{id}/routing", record)
                .with(token("DISPATCHER")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"branchId\":\"" + branch + "\",\"version\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.reason").exists());
        verifyNoInteractions(operations, leads);
    }

    @Test void conflictsRemain409WithReadableMessage() throws Exception {
        doThrow(new ConflictException("Запись изменена. Обновите данные", "VERSION_CONFLICT", null))
                .when(operations).updateFollowUp(eq(user), eq(record), any());
        mvc.perform(post("/dispatcher/operations/contracts/{id}/follow-up", record)
                .with(token("DISPATCHER")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\":\"COMPLETE\",\"version\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Запись изменена. Обновите данные"));
    }

    @Test void anonymousAndOtherRolesCannotReachDispatcherServices() throws Exception {
        mvc.perform(get("/dispatcher/operations/tasks")).andExpect(status().isUnauthorized());
        when(access.getAccess(user)).thenReturn(new EmployeeAccessDto(true, Set.of(Role.ADMIN), Set.of()));
        mvc.perform(get("/dispatcher/operations/tasks").with(token("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/dispatcher/leads/{id}/routing", record).with(token("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"branchId\":\"" + branch + "\",\"version\":0,\"reason\":\"Перевод\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(operations, leads);
    }

    @Test void disabledDispatcherCannotReadOperations() throws Exception {
        when(access.getAccess(user)).thenReturn(new EmployeeAccessDto(false, Set.of(Role.DISPATCHER), Set.of()));
        mvc.perform(get("/dispatcher/operations/tasks").with(token("DISPATCHER")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(operations);
    }

    @Test void allTimeReportsAcceptRequestsWithoutDateParameters() throws Exception {
        mvc.perform(get("/dispatcher/operations/reports").with(token("DISPATCHER")))
                .andExpect(status().isOk());
        verify(operations).report(user, null, null, null);
    }

    @Test void allTimeTrialsAcceptRequestsWithoutDateParameters() throws Exception {
        mvc.perform(get("/dispatcher/operations/trials").with(token("DISPATCHER")))
                .andExpect(status().isOk());
        verify(operations).trials(user, null, "", null, null, 0, 25);
    }

    @Test void invalidOrMissingRequiredParametersReturn400InsteadOf500() throws Exception {
        mvc.perform(get("/dispatcher/operations/assignees").with(token("DISPATCHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.parameter").value("branchId"));
        mvc.perform(get("/dispatcher/operations/reports").param("dateFrom", "2026-02-30")
                .with(token("DISPATCHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.parameter").value("dateFrom"));
        verifyNoInteractions(operations);
    }
}
