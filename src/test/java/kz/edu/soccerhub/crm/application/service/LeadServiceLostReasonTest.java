package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.lead.LeadLossReasonResponse;
import kz.edu.soccerhub.common.dto.lead.LeadLossReasonStage;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.crm.application.mapper.LeadMapper;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.crm.application.state.LeadStateMachineService;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadLossReasonEntity;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceLostReasonTest {

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
    void rejectWithoutLostReasonCodeShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.NEW);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(stateMachineService.process(leadId, LeadStatus.NEW, LeadEvent.REJECT)).thenReturn(LeadStatus.LOST);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> leadService.processEvent(leadId, LeadEvent.REJECT, null, null, adminId)
        );

        assertTrue(ex.getMessage().contains("lostReasonCode"));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void rejectWithUnknownLossReasonShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.NEW);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(stateMachineService.process(leadId, LeadStatus.NEW, LeadEvent.REJECT)).thenReturn(LeadStatus.LOST);
        when(leadLossReasonRepository.findByCodeAndActiveTrue("PRICE")).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> leadService.processEvent(leadId, LeadEvent.REJECT, "PRICE", "Too expensive", adminId)
        );

        assertTrue(ex.getMessage().contains("Invalid or inactive"));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void rejectWithValidReasonShouldPersistLostSnapshotAndActivity() {
        UUID leadId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.NEW);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(stateMachineService.process(leadId, LeadStatus.NEW, LeadEvent.REJECT)).thenReturn(LeadStatus.LOST);
        when(leadLossReasonPolicy.resolveStage(LeadEvent.REJECT, LeadStatus.NEW)).thenReturn(LeadLossReasonStage.PRE_QUALIFICATION);
        when(leadLossReasonPolicy.isAllowed("PRICE", LeadLossReasonStage.PRE_QUALIFICATION)).thenReturn(true);
        when(leadLossReasonRepository.findByCodeAndActiveTrue("PRICE"))
                .thenReturn(Optional.of(LeadLossReasonEntity.builder()
                        .code("PRICE")
                        .name("Цена")
                        .active(true)
                        .sortOrder(10)
                        .build()));

        LeadStatus status = leadService.processEvent(
                leadId,
                LeadEvent.REJECT,
                "PRICE",
                "Too expensive for parent",
                adminId
        );

        assertEquals(LeadStatus.LOST, status);
        assertEquals("PRICE", lead.getLostReasonCode());
        assertEquals("Too expensive for parent", lead.getLostComment());
        assertNotNull(lead.getLostAt());

        verify(leadRepository).save(lead);
        verify(leadActivityService).logStatusChanged(
                eq(lead),
                eq(LeadEvent.REJECT),
                eq(LeadStatus.NEW),
                eq(adminId),
                argThat(details -> details != null
                                   && details.contains("\"lostReasonCode\":\"PRICE\"")
                                   && details.contains("\"lostComment\":\"Too expensive for parent\""))
        );
    }

    @Test
    void rejectWithOtherReasonWithoutCommentShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.NEW);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(stateMachineService.process(leadId, LeadStatus.NEW, LeadEvent.REJECT)).thenReturn(LeadStatus.LOST);
        when(leadLossReasonRepository.findByCodeAndActiveTrue("OTHER"))
                .thenReturn(Optional.of(LeadLossReasonEntity.builder()
                        .code("OTHER")
                        .name("Другое")
                        .active(true)
                        .sortOrder(100)
                        .build()));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> leadService.processEvent(leadId, LeadEvent.REJECT, "OTHER", "   ", adminId)
        );

        assertTrue(ex.getMessage().contains("lostComment"));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void rejectWithReasonNotAllowedForStageShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.DECISION_PENDING);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(stateMachineService.process(leadId, LeadStatus.DECISION_PENDING, LeadEvent.REJECT)).thenReturn(LeadStatus.LOST);
        when(leadLossReasonRepository.findByCodeAndActiveTrue("NO_RESPONSE"))
                .thenReturn(Optional.of(LeadLossReasonEntity.builder()
                        .code("NO_RESPONSE")
                        .name("Не отвечает")
                        .active(true)
                        .sortOrder(40)
                        .build()));
        when(leadLossReasonPolicy.resolveStage(LeadEvent.REJECT, LeadStatus.DECISION_PENDING))
                .thenReturn(LeadLossReasonStage.POST_TRIAL_REJECT);
        when(leadLossReasonPolicy.isAllowed("NO_RESPONSE", LeadLossReasonStage.POST_TRIAL_REJECT)).thenReturn(false);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> leadService.processEvent(leadId, LeadEvent.REJECT, "NO_RESPONSE", "no answer", adminId)
        );

        assertTrue(ex.getMessage().contains("not allowed"));
        verify(leadRepository, never()).save(any());
    }

    @Test
    void getActiveLossReasonsShouldReturnOnlyActiveOrderedList() {
        when(leadLossReasonRepository.findByActiveTrueOrderBySortOrderAscNameAsc())
                .thenReturn(List.of(
                        LeadLossReasonEntity.builder().code("PRICE").name("Цена").active(true).sortOrder(10).build(),
                        LeadLossReasonEntity.builder().code("OTHER").name("Другое").active(true).sortOrder(100).build()
                ));
        when(leadLossReasonPolicy.resolveStagesForCode("PRICE"))
                .thenReturn(java.util.EnumSet.of(LeadLossReasonStage.PRE_QUALIFICATION, LeadLossReasonStage.PAYMENT_REJECT));
        when(leadLossReasonPolicy.resolveStagesForCode("OTHER"))
                .thenReturn(java.util.EnumSet.allOf(LeadLossReasonStage.class));
        when(leadLossReasonPolicy.isAllowed("PRICE", LeadLossReasonStage.PAYMENT_REJECT)).thenReturn(true);
        when(leadLossReasonPolicy.isAllowed("OTHER", LeadLossReasonStage.PAYMENT_REJECT)).thenReturn(true);

        List<LeadLossReasonResponse> reasons = leadService.getActiveLossReasons(LeadLossReasonStage.PAYMENT_REJECT);

        assertEquals(2, reasons.size());
        assertEquals("PRICE", reasons.get(0).code());
        assertEquals("Цена", reasons.get(0).name());
        assertTrue(reasons.get(0).stages().contains(LeadLossReasonStage.PAYMENT_REJECT));
        assertEquals("OTHER", reasons.get(1).code());
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
