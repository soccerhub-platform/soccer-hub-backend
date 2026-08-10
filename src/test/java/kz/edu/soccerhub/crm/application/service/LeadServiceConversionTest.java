package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.client.ClientConversionCommand;
import kz.edu.soccerhub.common.dto.client.ClientConversionOutput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadRequest;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadResponse;
import kz.edu.soccerhub.common.dto.lead.LeadConversionMode;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.TrialPort;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.crm.application.state.LeadStateMachineService;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadParticipant;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceConversionTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private ClientPort clientPort;

    @Mock
    private LeadActivityService leadActivityService;

    @Mock
    private TrialPort trialPort;

    @Mock
    private LeadStateMachineService leadStateMachineService;

    private DefaultLeadConversionService conversionService;

    @BeforeEach
    void setUp() {
        conversionService = new DefaultLeadConversionService(
                leadRepository,
                clientPort,
                leadActivityService,
                new ObjectMapper(),
                trialPort,
                leadStateMachineService
        );
    }

    @Test
    void convertsAfterTrialAndMovesLeadToContractPending() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        Lead lead = lead(
                leadId,
                LeadStatus.DECISION_PENDING,
                participantId,
                "Alex Doe"
        );

        ConvertLeadRequest request = request(
                participantId,
                null,
                LeadConversionMode.AFTER_TRIAL
        );

        when(leadRepository.findById(leadId))
                .thenReturn(Optional.of(lead));
        when(clientPort.convertLead(any(ClientConversionCommand.class)))
                .thenReturn(new ClientConversionOutput(
                        clientId,
                        playerId,
                        relationId
                ));
        when(leadStateMachineService.process(
                leadId,
                LeadStatus.DECISION_PENDING,
                LeadEvent.START_CONTRACT
        )).thenReturn(LeadStatus.CONTRACT_PENDING);

        ConvertLeadResponse response = conversionService.convertLeadToClient(
                leadId,
                request,
                adminId
        );

        assertEquals(LeadStatus.CONTRACT_PENDING, response.leadStatus());
        assertEquals("CONTRACT_PENDING", response.status());
        assertEquals(clientId, response.clientId());
        assertEquals(playerId, response.playerId());

        assertEquals(LeadStatus.CONTRACT_PENDING, lead.getStatus());
        assertEquals(clientId, lead.getClientId());
        assertEquals(
                playerId,
                lead.getParticipants().getFirst().getPlayerId()
        );

        verify(leadRepository).save(lead);
        verify(trialPort).linkConvertedStudent(any());
        verify(leadActivityService).logStatusChanged(
                org.mockito.ArgumentMatchers.eq(lead),
                org.mockito.ArgumentMatchers.eq(LeadEvent.START_CONTRACT),
                org.mockito.ArgumentMatchers.eq(LeadStatus.DECISION_PENDING),
                org.mockito.ArgumentMatchers.eq(adminId),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void convertsWithoutTrialUsingExistingClient() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID existingClientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();

        Lead lead = lead(
                leadId,
                LeadStatus.IN_PROGRESS,
                participantId,
                "Alex Doe"
        );

        ConvertLeadRequest request = request(
                participantId,
                existingClientId,
                LeadConversionMode.WITHOUT_TRIAL
        );

        when(leadRepository.findById(leadId))
                .thenReturn(Optional.of(lead));
        when(clientPort.convertLead(any(ClientConversionCommand.class)))
                .thenReturn(new ClientConversionOutput(
                        existingClientId,
                        playerId,
                        relationId
                ));
        when(leadStateMachineService.process(
                leadId,
                LeadStatus.IN_PROGRESS,
                LeadEvent.START_CONTRACT
        )).thenReturn(LeadStatus.CONTRACT_PENDING);

        ConvertLeadResponse response = conversionService.convertLeadToClient(
                leadId,
                request,
                UUID.randomUUID()
        );

        assertEquals(LeadStatus.CONTRACT_PENDING, response.leadStatus());
        assertEquals(existingClientId, response.clientId());

        ArgumentCaptor<ClientConversionCommand> commandCaptor =
                ArgumentCaptor.forClass(ClientConversionCommand.class);

        verify(clientPort).convertLead(commandCaptor.capture());
        assertEquals(
                existingClientId,
                commandCaptor.getValue().existingClientId()
        );

        verify(trialPort, never()).linkConvertedStudent(any());
    }

    @Test
    void rejectsAfterTrialModeFromInProgressLead() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        Lead lead = lead(
                leadId,
                LeadStatus.IN_PROGRESS,
                participantId,
                "Alex Doe"
        );

        when(leadRepository.findById(leadId))
                .thenReturn(Optional.of(lead));

        assertThrows(
                ConflictException.class,
                () -> conversionService.convertLeadToClient(
                        leadId,
                        request(
                                participantId,
                                null,
                                LeadConversionMode.AFTER_TRIAL
                        ),
                        UUID.randomUUID()
                )
        );

        verifyNoInteractions(clientPort, leadStateMachineService);
        verify(leadRepository, never()).save(any());
    }

    @Test
    void rejectsParticipantAlreadyLinkedToPlayer() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        Lead lead = lead(
                leadId,
                LeadStatus.IN_PROGRESS,
                participantId,
                "Alex Doe"
        );
        lead.getParticipants().getFirst().linkPlayer(UUID.randomUUID());

        when(leadRepository.findById(leadId))
                .thenReturn(Optional.of(lead));

        assertThrows(
                ConflictException.class,
                () -> conversionService.convertLeadToClient(
                        leadId,
                        request(
                                participantId,
                                null,
                                LeadConversionMode.WITHOUT_TRIAL
                        ),
                        UUID.randomUUID()
                )
        );

        verifyNoInteractions(clientPort, leadStateMachineService);
    }

    @Test
    void rejectsClientDifferentFromAlreadyLinkedClient() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        Lead lead = lead(
                leadId,
                LeadStatus.IN_PROGRESS,
                participantId,
                "Alex Doe"
        );
        lead.linkClient(UUID.randomUUID());

        when(leadRepository.findById(leadId))
                .thenReturn(Optional.of(lead));

        assertThrows(
                ConflictException.class,
                () -> conversionService.convertLeadToClient(
                        leadId,
                        request(
                                participantId,
                                UUID.randomUUID(),
                                LeadConversionMode.WITHOUT_TRIAL
                        ),
                        UUID.randomUUID()
                )
        );

        verifyNoInteractions(clientPort, leadStateMachineService);
    }

    @Test
    void rejectsParticipantFromAnotherLead() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        Lead lead = lead(
                leadId,
                LeadStatus.DECISION_PENDING,
                participantId,
                "Alex Doe"
        );

        when(leadRepository.findById(leadId))
                .thenReturn(Optional.of(lead));

        assertThrows(
                BadRequestException.class,
                () -> conversionService.convertLeadToClient(
                        leadId,
                        request(
                                UUID.randomUUID(),
                                null,
                                LeadConversionMode.AFTER_TRIAL
                        ),
                        UUID.randomUUID()
                )
        );

        verifyNoInteractions(clientPort, leadStateMachineService);
    }

    private ConvertLeadRequest request(
            UUID participantId,
            UUID existingClientId,
            LeadConversionMode conversionMode
    ) {
        return new ConvertLeadRequest(
                participantId,
                LocalDate.of(2016, 5, 20),
                ClientStudentRelationshipType.MOTHER,
                existingClientId,
                conversionMode,
                false,
                false
        );
    }

    private Lead lead(
            UUID leadId,
            LeadStatus status,
            UUID participantId,
            String participantName
    ) {
        Lead lead = Lead.builder()
                .id(leadId)
                .leadType(LeadType.CHILDREN)
                .primaryContactName("Parent One")
                .primaryContactPhone("+77001112233")
                .primaryContactEmail("parent@example.com")
                .source(LeadSource.OTHER)
                .status(status)
                .branchId(UUID.randomUUID())
                .build();

        lead.getParticipants().add(
                LeadParticipant.builder()
                        .id(participantId)
                        .lead(lead)
                        .fullName(participantName)
                        .birthDate(LocalDate.of(2016, 5, 20))
                        .gender(Gender.MALE)
                        .build()
        );

        return lead;
    }
}