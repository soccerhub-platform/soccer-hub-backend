package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.dto.lead.ScheduleTrialInput;
import kz.edu.soccerhub.common.dto.lead.TrialSlotInput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.crm.application.mapper.LeadMapper;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.crm.application.state.LeadStateMachineService;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.crm.domain.repository.LeadLossReasonRepository;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceWorkflowCleanupTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private LeadStateMachineService stateMachineService;
    @Mock
    private AdminPort adminPort;
    @Mock
    private GroupPort groupPort;
    @Mock
    private CoachPort coachPort;
    @Mock
    private GroupSchedulePort groupSchedulePort;
    @Mock
    private LeadConversionService leadConversionService;
    @Mock
    private LeadActivityService leadActivityService;
    @Mock
    private LeadMapper leadMapper;
    @Mock
    private LeadLossReasonRepository leadLossReasonRepository;
    @Mock
    private LeadLossReasonPolicy leadLossReasonPolicy;

    private LeadService leadService;

    @BeforeEach
    void setUp() {
        leadService = new LeadService(
                leadRepository,
                stateMachineService,
                adminPort,
                groupPort,
                coachPort,
                groupSchedulePort,
                leadConversionService,
                leadActivityService,
                leadMapper,
                leadLossReasonRepository,
                leadLossReasonPolicy,
                new ObjectMapper()
        );
    }

    @Test
    void manualConfirmPaymentShouldBeRejected() {
        UUID leadId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.WAITING_PAYMENT);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThrows(
                BadRequestException.class,
                () -> leadService.processEvent(leadId, LeadEvent.CONFIRM_PAYMENT, null, null, UUID.randomUUID())
        );

        verify(stateMachineService, never()).process(any(), any(), any());
    }

    @Test
    void requestPaymentShouldRequireContract() {
        UUID leadId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        assertThrows(
                BadRequestException.class,
                () -> leadService.processEvent(leadId, LeadEvent.REQUEST_PAYMENT, null, null, UUID.randomUUID())
        );

        verify(stateMachineService, never()).process(any(), any(), any());
    }

    @Test
    void cancelTrialShouldReturnLeadToQualifiedAndCancelTrial() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.TRIAL_SCHEDULED);
        lead.addParticipant("Alex Doe", LocalDate.of(2016, 5, 20), null, null);
        participantId = lead.getParticipants().getFirst().getId();
        lead.scheduleTrial(
                participantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 6, 30),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "trial"
        );

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(stateMachineService.process(leadId, LeadStatus.TRIAL_SCHEDULED, LeadEvent.CANCEL_TRIAL))
                .thenReturn(LeadStatus.QUALIFIED);

        LeadStatus status = leadService.processEvent(leadId, LeadEvent.CANCEL_TRIAL, null, null, UUID.randomUUID());

        assertEquals(LeadStatus.QUALIFIED, status);
        assertEquals(LeadStatus.QUALIFIED, lead.getStatus());
        assertEquals(kz.edu.soccerhub.crm.domain.model.enums.LeadTrialStatus.CANCELED, lead.getTrial().getStatus());
    }

    @Test
    void scheduleTrialShouldSupportRescheduleWithoutStateTransition() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.TRIAL_SCHEDULED);
        lead.addParticipant("Alex Doe", LocalDate.of(2016, 5, 20), null, null);
        participantId = lead.getParticipants().getFirst().getId();
        lead.scheduleTrial(
                participantId,
                groupId,
                coachId,
                LocalDate.of(2026, 6, 30),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "initial"
        );

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupSchedulePort.getActiveSchedulesByGroup(groupId, LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(slot(groupId, coachId, LocalDate.of(2026, 7, 1), LocalTime.of(12, 0), LocalTime.of(13, 0))));
        when(groupSchedulePort.getActiveSchedulesByCoach(coachId, LocalDate.of(2026, 7, 1)))
                .thenReturn(List.of(slot(groupId, coachId, LocalDate.of(2026, 7, 1), LocalTime.of(12, 0), LocalTime.of(13, 0))));
        when(coachPort.verifyCoach(coachId)).thenReturn(true);
        when(groupPort.getGroupById(groupId)).thenReturn(
                kz.edu.soccerhub.common.dto.group.GroupDto.builder()
                        .groupId(groupId)
                        .branchId(lead.getBranchId())
                        .build()
        );

        leadService.scheduleTrial(
                leadId,
                new ScheduleTrialInput(
                        participantId,
                        groupId,
                        coachId,
                        new TrialSlotInput(LocalDate.of(2026, 7, 1), LocalTime.of(12, 0)),
                        "rescheduled"
                ),
                UUID.randomUUID()
        );

        assertEquals(LeadStatus.TRIAL_SCHEDULED, lead.getStatus());
        assertEquals(LocalDate.of(2026, 7, 1), lead.getTrial().getTrialDate());
        assertEquals(LocalTime.of(12, 0), lead.getTrial().getStartTime());
        verify(stateMachineService, never()).process(eq(leadId), eq(LeadStatus.TRIAL_SCHEDULED), eq(LeadEvent.SCHEDULE_TRIAL));
    }

    private GroupScheduleDto slot(UUID groupId, UUID coachId, LocalDate date, LocalTime start, LocalTime end) {
        return GroupScheduleDto.builder()
                .scheduleId(UUID.randomUUID())
                .groupId(groupId)
                .coachId(coachId)
                .branchId(UUID.randomUUID())
                .dayOfWeek(date.getDayOfWeek())
                .startTime(start)
                .endTime(end)
                .startDate(date.minusDays(7))
                .endDate(date.plusDays(7))
                .scheduleType("REGULAR")
                .status("ACTIVE")
                .substitution(false)
                .build();
    }

    private Lead lead(UUID id, LeadStatus status) {
        return Lead.builder()
                .id(id)
                .leadType(LeadType.CHILDREN)
                .primaryContactName("Parent")
                .primaryContactPhone("+77001112233")
                .primaryContactEmail("parent@example.com")
                .source(LeadSource.OTHER)
                .status(status)
                .branchId(UUID.randomUUID())
                .build();
    }
}
