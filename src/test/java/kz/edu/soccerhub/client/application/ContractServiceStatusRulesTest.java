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
import kz.edu.soccerhub.common.dto.contract.ContractExtendCommand;
import kz.edu.soccerhub.common.dto.contract.ContractUpdateCommand;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.exception.ContractValidationException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private GroupMembershipSyncService groupMembershipSyncService;

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
                groupMembershipSyncService
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
