package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractSnapshotServiceTest {

    @Mock ContractRepository contractRepository;
    @Mock PlayerRepository playerRepository;
    @Mock ClientRepository clientRepository;
    @Mock GroupPort groupPort;
    @Mock CoachPort coachPort;
    @InjectMocks ContractSnapshotService service;

    @Test
    void shouldBuildStudentSnapshotForContractWithoutGroup() {
        UUID branchId = UUID.randomUUID();
        Client client = Client.builder().id(UUID.randomUUID()).branchId(branchId).build();
        Player player = Player.builder().id(UUID.randomUUID()).firstName("Alex").lastName("Doe").build();
        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .clientId(client.getId())
                .playerId(player.getId())
                .contractNumber("CNT-2026-00001")
                .leadType(LeadType.ADULT)
                .status(ContractStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .amount(BigDecimal.valueOf(30000))
                .currency("KZT")
                .build();

        when(contractRepository.findByPlayerId(player.getId())).thenReturn(List.of(contract));
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        when(playerRepository.findByIdIn(Set.of(player.getId()))).thenReturn(List.of(player));
        when(clientRepository.findAllById(Set.of(client.getId()))).thenReturn(List.of(client));

        var output = service.getStudentContracts(branchId, player.getId());

        assertEquals(1, output.size());
        assertNull(output.getFirst().groupId());
        assertNull(output.getFirst().groupName());
        verifyNoInteractions(groupPort);
    }
}
