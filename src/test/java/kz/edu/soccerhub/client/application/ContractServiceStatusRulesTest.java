package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ContractHistoryRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.dto.contract.ContractDetailsOutput;
import kz.edu.soccerhub.common.dto.contract.ContractCreateCommand;
import kz.edu.soccerhub.common.dto.contract.ContractParticipantDraftInput;
import kz.edu.soccerhub.common.dto.contract.ContractPrimaryContactDraftInput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.common.dto.contract.ContractExtendCommand;
import kz.edu.soccerhub.common.dto.contract.ContractUpdateCommand;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentContextOutput;
import kz.edu.soccerhub.common.exception.ContractValidationException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.ClientStudentRelationPort;
import kz.edu.soccerhub.common.port.ClientActivityPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.organization.application.service.GroupMembershipSyncService;
import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceStatusRulesTest {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ContractHistoryRepository contractHistoryRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private GroupPort groupPort;
    @Mock
    private GroupCoachPort groupCoachPort;
    @Mock
    private CoachPort coachPort;
    @Mock
    private AdminPort adminPort;
    @Mock
    private AuthPort authPort;
    @Mock
    private LeadPort leadPort;
    @Mock
    private ClientStudentRelationPort relationPort;
    @Mock
    private ClientActivityPort clientActivityPort;
    @Mock
    private GroupMembershipSyncService groupMembershipSyncService;
    @Mock
    private ClientStudentRelationSyncService relationSyncService;

    private ContractService contractService;

    @BeforeEach
    void setUp() {
        contractService = new ContractService(
                contractRepository,
                contractHistoryRepository,
                playerRepository,
                clientRepository,
                groupPort,
                groupCoachPort,
                coachPort,
                adminPort,
                authPort,
                leadPort,
                relationPort,
                clientActivityPort,
                groupMembershipSyncService,
                relationSyncService
        );
    }

    @Test
    void updateExpiredContractShouldBeRejected() {
        Contract contract = contract(ContractStatus.EXPIRED, LocalDate.now().minusDays(30), LocalDate.now().minusDays(1));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

        ContractValidationException ex = assertThrows(
                ContractValidationException.class,
                () -> contractService.update(contract.getId(), updateCommand(contract), UUID.randomUUID())
        );

        assertEquals("IMMUTABLE_STATUS", ex.getErrors().getFirst().code());
        assertTrue(ex.getErrors().getFirst().message().contains("EXPIRED"));
    }

    @Test
    void extendExpiredContractShouldBeAllowed() {
        UUID actorUserId = UUID.randomUUID();
        LocalDate newEndDate = LocalDate.now().plusMonths(1);
        Contract contract = contract(ContractStatus.EXPIRED, LocalDate.now().minusMonths(2), LocalDate.now().minusDays(1));
        Player player = player(contract.getPlayerId(), client());

        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepository.existsOverlappingContractInRange(player.getId(), contract.getStartDate(), newEndDate, contract.getId()))
                .thenReturn(false);
        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(groupPort.getGroupById(contract.getGroupId())).thenReturn(group(contract.getGroupId(), player.getParent().getBranchId()));
        when(contractHistoryRepository.findByContractIdOrderByCreatedAtDesc(contract.getId())).thenReturn(List.of());
        when(adminPort.findById(actorUserId)).thenReturn(Optional.empty());

        ContractDetailsOutput output = contractService.extend(
                contract.getId(),
                new ContractExtendCommand(newEndDate, BigDecimal.valueOf(25000), "renewed"),
                actorUserId
        );

        assertEquals(newEndDate, contract.getEndDate());
        assertEquals(ContractStatus.ACTIVE, contract.getStatus());
        assertEquals(ContractStatus.ACTIVE, output.status());
        verify(contractHistoryRepository).save(any());
        verify(clientActivityPort).recordClientActivity(
                org.mockito.ArgumentMatchers.eq(player.getParent().getId()),
                org.mockito.ArgumentMatchers.eq(actorUserId),
                org.mockito.ArgumentMatchers.eq(ClientActivityType.CONTRACT_EXTENDED),
                any()
        );
    }

    @Test
    void extendCancelledContractShouldBeRejected() {
        Contract contract = contract(ContractStatus.CANCELLED, LocalDate.now().minusMonths(2), LocalDate.now().plusDays(2));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

        ContractValidationException ex = assertThrows(
                ContractValidationException.class,
                () -> contractService.extend(
                        contract.getId(),
                        new ContractExtendCommand(LocalDate.now().plusMonths(1), BigDecimal.TEN, "nope"),
                        UUID.randomUUID()
                )
        );

        assertEquals("IMMUTABLE_STATUS", ex.getErrors().getFirst().code());
        assertTrue(ex.getErrors().getFirst().message().contains("CANCELLED"));
    }

    @Test
    void paymentContextShouldUseExplicitContractClient() {
        Client legacyParent = client();
        Client contractClient = client();
        Contract contract = contract(ContractStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusMonths(1));
        contract.setClientId(contractClient.getId());
        Player player = player(contract.getPlayerId(), legacyParent);

        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(clientRepository.findById(contractClient.getId())).thenReturn(Optional.of(contractClient));

        ContractPaymentContextOutput output = contractService.getPaymentContext(contract.getId());

        assertEquals(contractClient.getId(), output.clientId());
        assertEquals(contractClient.getBranchId(), output.branchId());
    }

    @Test
    void participantLookupShouldUseActiveClientRelations() {
        Client selectedClient = client();
        Player linkedPlayer = player(UUID.randomUUID(), client());
        ClientStudentRelationOutput relation = new ClientStudentRelationOutput(
                UUID.randomUUID(), selectedClient.getId(), "Jane Doe", linkedPlayer.getId(), "Alex Doe",
                ClientStudentRelationshipType.MOTHER, true, true, true, true,
                LocalDate.now().minusMonths(1), null, true
        );

        when(clientRepository.findById(selectedClient.getId())).thenReturn(Optional.of(selectedClient));
        when(relationPort.getClientStudents(selectedClient.getId())).thenReturn(List.of(relation));
        when(playerRepository.findAllById(List.of(linkedPlayer.getId()))).thenReturn(List.of(linkedPlayer));
        when(contractRepository.findTopByPlayerIdOrderByCreatedAtDesc(linkedPlayer.getId())).thenReturn(Optional.empty());
        when(leadPort.getLatestLeadTypesByParticipantIds(List.of(linkedPlayer.getId())))
                .thenReturn(Map.of(linkedPlayer.getId(), LeadType.CHILDREN));

        var participants = contractService.getParticipants(selectedClient.getBranchId(), selectedClient.getId());

        assertEquals(1, participants.size());
        assertEquals(linkedPlayer.getId(), participants.getFirst().id());
        assertEquals(selectedClient.getId(), participants.getFirst().primaryContact().id());
    }

    @Test
    void createContractShouldAcceptClientLinkedThroughRelationInsteadOfLegacyParent() {
        UUID actorId = UUID.randomUUID();
        Client selectedClient = client();
        Client legacyParent = client();
        Player linkedPlayer = player(UUID.randomUUID(), legacyParent);
        UUID groupId = UUID.randomUUID();
        ContractCreateCommand command = new ContractCreateCommand(
                selectedClient.getBranchId(), LeadType.CHILDREN, linkedPlayer.getId(), null,
                selectedClient.getId(), null, null, groupId, null, "CNT-RELATION-1",
                LocalDate.now(), LocalDate.now().plusMonths(1), BigDecimal.valueOf(30000), "KZT", null
        );
        ClientStudentRelationOutput relation = new ClientStudentRelationOutput(
                UUID.randomUUID(), selectedClient.getId(), "Jane Doe", linkedPlayer.getId(), "Alex Doe",
                ClientStudentRelationshipType.MOTHER, true, true, true, true,
                LocalDate.now().minusDays(1), null, true
        );

        when(playerRepository.findById(linkedPlayer.getId())).thenReturn(Optional.of(linkedPlayer));
        when(clientRepository.findById(selectedClient.getId())).thenReturn(Optional.of(selectedClient));
        when(relationPort.getStudentClients(linkedPlayer.getId())).thenReturn(List.of(relation));
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, selectedClient.getBranchId()));
        when(contractRepository.existsOverlappingContractInRange(
                linkedPlayer.getId(), command.startDate(), command.endDate(), null
        )).thenReturn(false);
        when(contractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractHistoryRepository.findByContractIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(adminPort.findById(actorId)).thenReturn(Optional.empty());

        ContractDetailsOutput output = contractService.create(command, actorId);

        assertEquals(selectedClient.getId(), output.primaryContact().id());
        verify(clientActivityPort).recordClientActivity(
                org.mockito.ArgumentMatchers.eq(selectedClient.getId()),
                org.mockito.ArgumentMatchers.eq(actorId),
                org.mockito.ArgumentMatchers.eq(ClientActivityType.CONTRACT_CREATED),
                any()
        );
    }

    @Test
    void createContractWithDraftsShouldCreateExplicitClientStudentRelation() {
        UUID actorId = UUID.randomUUID();
        Client client = client();
        UUID groupId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(1);
        ContractCreateCommand command = new ContractCreateCommand(
                client.getBranchId(), LeadType.CHILDREN, null,
                new ContractParticipantDraftInput("Alex Doe", LocalDate.of(2015, 5, 10)),
                null,
                new ContractPrimaryContactDraftInput("Jane Doe", client.getPhone(), null, "Referral", "Onboarding"),
                ClientStudentRelationshipType.MOTHER,
                groupId, null, null, startDate, endDate, BigDecimal.valueOf(30000), "KZT", null
        );

        when(clientRepository.findByPhone(client.getPhone())).thenReturn(Optional.of(client));
        when(playerRepository.findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(
                client.getId(), "Alex", "Doe", LocalDate.of(2015, 5, 10)
        )).thenReturn(Optional.empty());
        when(playerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(relationPort.getStudentClients(any())).thenReturn(List.of());
        when(groupPort.getGroupById(groupId)).thenReturn(group(groupId, client.getBranchId()));
        when(contractRepository.existsOverlappingContractInRange(any(), any(), any(), any())).thenReturn(false);
        when(contractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractHistoryRepository.findByContractIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(adminPort.findById(actorId)).thenReturn(Optional.empty());

        ContractDetailsOutput output = contractService.create(command, actorId);

        assertEquals(client.getId(), output.primaryContact().id());
        verify(relationPort).create(argThat(relation ->
                relation.clientId().equals(client.getId())
                        && relation.relationshipType() == ClientStudentRelationshipType.MOTHER
                        && relation.primaryContact()
                        && relation.primaryPayer()
        ));
    }

    private ContractUpdateCommand updateCommand(Contract contract) {
        return new ContractUpdateCommand(
                UUID.randomUUID(),
                LeadType.CHILDREN,
                contract.getPlayerId(),
                UUID.randomUUID(),
                contract.getGroupId(),
                null,
                contract.getContractNumber(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getAmount(),
                contract.getCurrency(),
                contract.getNotes()
        );
    }

    private Contract contract(ContractStatus status, LocalDate startDate, LocalDate endDate) {
        return Contract.builder()
                .id(UUID.randomUUID())
                .playerId(UUID.randomUUID())
                .groupId(UUID.randomUUID())
                .contractNumber("CNT-2026-00001")
                .leadType(LeadType.CHILDREN)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .amount(BigDecimal.valueOf(15000))
                .currency("KZT")
                .notes("note")
                .build();
    }

    private Player player(UUID playerId, Client client) {
        return Player.builder()
                .id(playerId)
                .firstName("Alex")
                .lastName("Doe")
                .birthDate(LocalDate.of(2015, 5, 10))
                .parent(client)
                .build();
    }

    private Client client() {
        return Client.builder()
                .id(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Doe")
                .phone("+77010000000")
                .branchId(UUID.randomUUID())
                .build();
    }

    private GroupDto group(UUID groupId, UUID branchId) {
        return GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Group A")
                .audienceType(GroupAudienceType.CHILDREN)
                .status(GroupStatus.ACTIVE)
                .build();
    }
}
