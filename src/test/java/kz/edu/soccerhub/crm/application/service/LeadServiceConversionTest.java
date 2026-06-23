package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.client.ClientConversionOutput;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadRequest;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadResponse;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadParticipant;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceConversionTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private GroupPort groupPort;
    @Mock
    private ClientPort clientPort;
    @Mock
    private LeadActivityService leadActivityService;

    private DefaultLeadConversionService conversionService;

    @BeforeEach
    void setUp() {
        conversionService = new DefaultLeadConversionService(
                leadRepository,
                groupPort,
                clientPort,
                leadActivityService,
                new ObjectMapper()
        );
    }

    @Test
    void convertValidLeadCreatesClientPlayerContractAndUpdatesLead() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, branchId, participantId, "Alex Doe");
        ConvertLeadRequest request = request(participantId, groupId);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, branchId));
        when(clientPort.convertLead(any())).thenReturn(new ClientConversionOutput(clientId, playerId, contractId));

        ConvertLeadResponse response = conversionService.convertLeadToClient(leadId, request, actorId);

        assertEquals(leadId, response.leadId());
        assertEquals(clientId, response.clientId());
        assertEquals(playerId, response.playerId());
        assertEquals(contractId, response.contractId());
        assertEquals("CONVERTED", response.status());
        assertEquals(LeadStatus.WAITING_PAYMENT, lead.getStatus());
        assertEquals(clientId, lead.getClientId());
        assertEquals(playerId, lead.getParticipantId());
        assertEquals(contractId, lead.getContractId());

        verify(leadRepository).save(lead);
        verify(clientPort).convertLead(argThat(command ->
                command.existingClientId() == null
                        && command.groupId().equals(groupId)
                        && command.participantName().equals("Alex Doe")
                        && command.participantBirthDate().equals(request.participantBirthDate())
        ));
        verify(leadActivityService).logLeadConverted(eq(lead), eq(actorId), eq(LeadStatus.TRIAL_DONE), contains("\"participantId\""));
    }

    @Test
    void convertFromWaitingPaymentShouldBeAllowedForAlreadyConvertedLead() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID existingClientId = UUID.randomUUID();
        UUID existingPlayerId = UUID.randomUUID();
        UUID existingContractId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.WAITING_PAYMENT, branchId, participantId, "Alex Doe");
        lead.markConverted(existingClientId, existingPlayerId, existingContractId);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, branchId));
        when(clientPort.convertLead(any())).thenReturn(new ClientConversionOutput(
                existingClientId,
                existingPlayerId,
                existingContractId
        ));

        ConvertLeadResponse response = conversionService.convertLeadToClient(leadId, request(participantId, groupId), actorId);

        assertEquals("CONVERTED", response.status());
        assertEquals(LeadStatus.WAITING_PAYMENT, lead.getStatus());
        verify(leadRepository).save(lead);
    }

    @Test
    void convertFromTrialDoneWithoutPaymentShouldMoveLeadToWon() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, branchId, participantId, "Alex Doe");

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, branchId));
        when(clientPort.convertLead(any())).thenReturn(new ClientConversionOutput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        ));

        ConvertLeadRequest request = new ConvertLeadRequest(
                participantId,
                groupId,
                LocalDate.of(2016, 5, 20),
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 6, 20),
                BigDecimal.ZERO
        );

        conversionService.convertLeadToClient(leadId, request, actorId);

        assertEquals(LeadStatus.WON, lead.getStatus());
    }

    @Test
    void convertParticipantFromAnotherLeadShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, UUID.randomUUID(), UUID.randomUUID(), "Foreign Kid");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> conversionService.convertLeadToClient(leadId, request(participantId, UUID.randomUUID()), UUID.randomUUID())
        );

        assertTrue(ex.getMessage().contains("Participant does not belong"));
    }

    @Test
    void convertWithGroupFromAnotherBranchShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID leadBranch = UUID.randomUUID();
        UUID otherBranch = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, leadBranch, participantId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, otherBranch));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> conversionService.convertLeadToClient(leadId, request(participantId, groupId), UUID.randomUUID())
        );

        assertTrue(ex.getMessage().contains("does not belong to lead branch"));
    }

    @Test
    void convertWithMissingGroupShouldReturnNotFound() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, UUID.randomUUID(), participantId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenThrow(new NotFoundException("Group not found", groupId));

        assertThrows(
                NotFoundException.class,
                () -> conversionService.convertLeadToClient(leadId, request(participantId, groupId), UUID.randomUUID())
        );
    }

    @Test
    void convertAlreadyConvertedLeadShouldReuseExistingClientId() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.WON, branchId, participantId, "Alex Doe");
        lead.markConverted(clientId, playerId, contractId);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, branchId));
        when(clientPort.convertLead(any())).thenReturn(new ClientConversionOutput(clientId, playerId, contractId));

        ConvertLeadResponse response = conversionService.convertLeadToClient(
                leadId,
                request(participantId, groupId),
                UUID.randomUUID()
        );

        assertEquals(clientId, response.clientId());
        assertEquals(playerId, response.playerId());
        assertEquals(contractId, response.contractId());
        verify(clientPort).convertLead(argThat(command -> command.existingClientId().equals(clientId)));
    }

    @Test
    void convertFromTrialScheduledShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_SCHEDULED, UUID.randomUUID(), participantId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> conversionService.convertLeadToClient(leadId, request(participantId, groupId), UUID.randomUUID())
        );

        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    void convertWithoutParticipantBirthDateShouldFailValidation() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, UUID.randomUUID(), participantId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        ConvertLeadRequest request = new ConvertLeadRequest(
                participantId,
                groupId,
                null,
                LocalDate.of(2026, 5, 20),
                null,
                null
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> conversionService.convertLeadToClient(leadId, request, UUID.randomUUID())
        );
        assertTrue(ex.getMessage().contains("participantBirthDate"));
    }

    private ConvertLeadRequest request(UUID participantId, UUID groupId) {
        return new ConvertLeadRequest(
                participantId,
                groupId,
                LocalDate.of(2016, 5, 20),
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 6, 20),
                new BigDecimal("30000")
        );
    }

    private GroupDto group(UUID groupId, UUID branchId) {
        return GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("U10")
                .status(GroupStatus.ACTIVE)
                .build();
    }

    private Lead lead(UUID leadId, LeadStatus status, UUID branchId, UUID participantId, String participantName) {
        Lead lead = Lead.builder()
                .id(leadId)
                .leadType(LeadType.CHILDREN)
                .primaryContactName("Parent One")
                .primaryContactPhone("+77001112233")
                .primaryContactEmail("parent@example.com")
                .source(LeadSource.OTHER)
                .status(status)
                .branchId(branchId)
                .build();

        LeadParticipant participant = LeadParticipant.builder()
                .id(participantId)
                .lead(lead)
                .fullName(participantName)
                .birthDate(LocalDate.of(2016, 5, 20))
                .build();
        lead.getParticipants().add(participant);
        return lead;
    }
}
