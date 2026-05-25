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
import kz.edu.soccerhub.crm.domain.model.LeadChild;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
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
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, branchId, childId, "Alex Doe");
        ConvertLeadRequest request = request(childId, groupId);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, branchId));
        when(clientPort.convertLead(any())).thenReturn(new ClientConversionOutput(clientId, playerId, contractId));

        ConvertLeadResponse response = conversionService.convertLeadToClient(leadId, request, actorId);

        assertEquals(leadId, response.leadId());
        assertEquals(clientId, response.clientId());
        assertEquals(playerId, response.playerId());
        assertEquals(contractId, response.contractId());
        assertEquals("CONVERTED", response.status());
        assertEquals(LeadStatus.WON, lead.getStatus());
        assertEquals(clientId, lead.getClientId());
        assertEquals(playerId, lead.getPlayerId());
        assertEquals(contractId, lead.getContractId());

        verify(leadRepository).save(lead);
        verify(clientPort).convertLead(argThat(command ->
                command.existingClientId() == null
                        && command.groupId().equals(groupId)
                        && command.childName().equals("Alex Doe")
                        && command.childBirthDate().equals(request.childBirthDate())
        ));
        verify(leadActivityService).logLeadConverted(eq(lead), eq(actorId), eq(LeadStatus.TRIAL_DONE), contains("\"childId\""));
    }

    @Test
    void convertFromWaitingPaymentShouldBeAllowed() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.WAITING_PAYMENT, branchId, childId, "Alex Doe");

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, branchId));
        when(clientPort.convertLead(any())).thenReturn(new ClientConversionOutput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        ));

        ConvertLeadResponse response = conversionService.convertLeadToClient(leadId, request(childId, groupId), actorId);

        assertEquals("CONVERTED", response.status());
        assertEquals(LeadStatus.WON, lead.getStatus());
        verify(leadRepository).save(lead);
    }

    @Test
    void convertChildFromAnotherLeadShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, UUID.randomUUID(), UUID.randomUUID(), "Foreign Kid");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> conversionService.convertLeadToClient(leadId, request(childId, UUID.randomUUID()), UUID.randomUUID())
        );

        assertTrue(ex.getMessage().contains("Child does not belong"));
    }

    @Test
    void convertWithGroupFromAnotherBranchShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID leadBranch = UUID.randomUUID();
        UUID otherBranch = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, leadBranch, childId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, otherBranch));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> conversionService.convertLeadToClient(leadId, request(childId, groupId), UUID.randomUUID())
        );

        assertTrue(ex.getMessage().contains("does not belong to lead branch"));
    }

    @Test
    void convertWithMissingGroupShouldReturnNotFound() {
        UUID leadId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, UUID.randomUUID(), childId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenThrow(new NotFoundException("Group not found", groupId));

        assertThrows(
                NotFoundException.class,
                () -> conversionService.convertLeadToClient(leadId, request(childId, groupId), UUID.randomUUID())
        );
    }

    @Test
    void convertAlreadyConvertedLeadShouldReuseExistingClientId() {
        UUID leadId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.WON, branchId, childId, "Alex Doe");
        lead.markConverted(clientId, playerId, contractId);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, branchId));
        when(clientPort.convertLead(any())).thenReturn(new ClientConversionOutput(clientId, playerId, contractId));

        ConvertLeadResponse response = conversionService.convertLeadToClient(
                leadId,
                request(childId, groupId),
                UUID.randomUUID()
        );

        assertEquals(clientId, response.clientId());
        assertEquals(playerId, response.playerId());
        assertEquals(contractId, response.contractId());
        verify(clientPort).convertLead(argThat(command -> command.existingClientId().equals(clientId)));
    }

    @Test
    void convertWithoutChildBirthDateShouldFailValidation() {
        UUID leadId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, UUID.randomUUID(), childId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        ConvertLeadRequest request = new ConvertLeadRequest(
                childId,
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
        assertTrue(ex.getMessage().contains("childBirthDate"));
    }

    private ConvertLeadRequest request(UUID childId, UUID groupId) {
        return new ConvertLeadRequest(
                childId,
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

    private Lead lead(UUID leadId, LeadStatus status, UUID branchId, UUID childId, String childName) {
        Lead lead = Lead.builder()
                .id(leadId)
                .parentName("Parent One")
                .phone("+77001112233")
                .email("parent@example.com")
                .source(LeadSource.OTHER)
                .status(status)
                .branchId(branchId)
                .build();

        LeadChild child = LeadChild.builder()
                .id(childId)
                .lead(lead)
                .childName(childName)
                .childAge(10)
                .build();
        lead.getChildren().add(child);
        return lead;
    }
}
