package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ContractHistoryRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.common.dto.contract.ContractCreateCommand;
import kz.edu.soccerhub.common.dto.contract.ContractSearchQuery;
import kz.edu.soccerhub.common.dto.contract.ContractUpdateCommand;
import kz.edu.soccerhub.common.exception.ContractValidationException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.ClientActivityPort;
import kz.edu.soccerhub.common.port.ClientStudentRelationPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceStatusRulesTest {

    @Mock ContractRepository contractRepository;
    @Mock ContractHistoryRepository contractHistoryRepository;
    @Mock PlayerRepository playerRepository;
    @Mock ClientRepository clientRepository;
    @Mock GroupPort groupPort;
    @Mock GroupCoachPort groupCoachPort;
    @Mock CoachPort coachPort;
    @Mock AdminPort adminPort;
    @Mock LeadPort leadPort;
    @Mock ClientStudentRelationPort relationPort;
    @Mock ClientActivityPort clientActivityPort;

    private ContractService service;

    @BeforeEach
    void setUp() {
        service = new ContractService(
                contractRepository,
                contractHistoryRepository,
                playerRepository,
                clientRepository,
                groupPort,
                groupCoachPort,
                coachPort,
                adminPort,
                leadPort,
                relationPort,
                clientActivityPort
        );
    }

    @Test
    void createShouldBuildDraftForExistingRelatedPartiesWithoutGroup() {
        UUID actorId = UUID.randomUUID();
        Client client = client();
        Player player = player();
        ContractCreateCommand command = createCommand(client, player);

        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(relationPort.getStudentClients(player.getId())).thenReturn(List.of(relation(client, player)));
        when(contractRepository.findTopByPlayerIdOrderByCreatedAtDesc(player.getId())).thenReturn(Optional.empty());
        when(leadPort.getLatestLeadTypesByParticipantIds(List.of(player.getId())))
                .thenReturn(Map.of(player.getId(), LeadType.ADULT));
        when(contractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractHistoryRepository.findByContractIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(adminPort.findById(actorId)).thenReturn(Optional.empty());

        var output = service.create(command, actorId);

        assertEquals(ContractStatus.DRAFT, output.status());
        assertEquals(client.getId(), output.primaryContact().id());
        assertEquals(player.getId(), output.participant().id());
        assertNull(output.group());
        assertEquals(true, output.capabilities().canActivate());
    }

    @Test
    void createShouldRejectClientWithoutActiveStudentRelation() {
        Client client = client();
        Player player = player();
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(relationPort.getStudentClients(player.getId())).thenReturn(List.of());

        assertThrows(ContractValidationException.class, () -> service.create(
                createCommand(client, player), UUID.randomUUID()
        ));
    }

    @Test
    void activateShouldValidateOverlapAndChangePersistedStatus() {
        UUID actorId = UUID.randomUUID();
        Client client = client();
        Player player = player();
        Contract contract = contract(client, player, ContractStatus.DRAFT);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepository.existsOverlappingContractInRange(
                player.getId(), contract.getStartDate(), contract.getEndDate(), contract.getId()
        )).thenReturn(false);
        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        when(contractHistoryRepository.findByContractIdOrderByCreatedAtDesc(contract.getId())).thenReturn(List.of());
        when(adminPort.findById(actorId)).thenReturn(Optional.empty());

        var output = service.activate(contract.getId(), actorId);

        assertEquals(ContractStatus.ACTIVE, output.status());
        assertEquals(false, output.capabilities().canEdit());
    }

    @Test
    void updateShouldOnlyAllowDrafts() {
        Client client = client();
        Player player = player();
        Contract contract = contract(client, player, ContractStatus.ACTIVE);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

        assertThrows(ContractValidationException.class, () -> service.update(
                contract.getId(),
                new ContractUpdateCommand(
                        contract.getStartDate(),
                        contract.getEndDate(),
                        contract.getAmount(),
                        contract.getCurrency(),
                        contract.getNotes()
                ),
                UUID.randomUUID()
        ));
    }

    @Test
    void searchShouldReturnContractWithoutGroup() {
        Client client = client();
        Player player = player();
        Contract contract = contract(client, player, ContractStatus.ACTIVE);
        PageRequest pageable = PageRequest.of(0, 20);

        when(contractRepository.search(
                client.getBranchId(), null, null, Set.of(), true, null, null, pageable
        )).thenReturn(new PageImpl<>(List.of(contract), pageable, 1));
        when(playerRepository.findByIdIn(Set.of(player.getId()))).thenReturn(List.of(player));
        when(clientRepository.findAllById(Set.of(client.getId()))).thenReturn(List.of(client));

        var output = service.search(
                new ContractSearchQuery(client.getBranchId(), null, Set.of(), null, null),
                pageable
        );

        assertEquals(1, output.content().size());
        assertNull(output.content().getFirst().group());
        assertEquals(client.getId(), output.content().getFirst().primaryContact().id());
    }

    private ContractCreateCommand createCommand(Client client, Player player) {
        return new ContractCreateCommand(
                client.getBranchId(),
                client.getId(),
                player.getId(),
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                BigDecimal.valueOf(30000),
                "KZT",
                "New agreement"
        );
    }

    private ClientStudentRelationOutput relation(Client client, Player player) {
        return new ClientStudentRelationOutput(
                UUID.randomUUID(),
                client.getId(),
                "Jane Doe",
                player.getId(),
                "Alex Doe",
                ClientStudentRelationshipType.SELF,
                true,
                true,
                true,
                true,
                LocalDate.now(),
                null,
                true
        );
    }

    private Contract contract(Client client, Player player, ContractStatus status) {
        return Contract.builder()
                .id(UUID.randomUUID())
                .clientId(client.getId())
                .playerId(player.getId())
                .contractNumber("CNT-2026-00001")
                .leadType(LeadType.ADULT)
                .status(status)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .amount(BigDecimal.valueOf(30000))
                .currency("KZT")
                .notes("note")
                .build();
    }

    private Client client() {
        return Client.builder()
                .id(UUID.randomUUID())
                .branchId(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Doe")
                .build();
    }

    private Player player() {
        return Player.builder()
                .id(UUID.randomUUID())
                .firstName("Alex")
                .lastName("Doe")
                .birthDate(LocalDate.of(1995, 5, 10))
                .build();
    }
}
