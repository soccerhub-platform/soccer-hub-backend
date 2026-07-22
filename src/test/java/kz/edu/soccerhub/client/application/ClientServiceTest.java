package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.ClientStudentRelation;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ClientStudentRelationRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.dto.student.StudentUpdateCommand;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.GroupMembershipPort;
import kz.edu.soccerhub.organization.domain.model.GroupMembership;
import kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private BranchPort branchPort;
    @Mock
    private AuthPort authPort;
    @Mock
    private GroupMembershipPort groupMembershipPort;
    @Mock
    private ClientStudentRelationSyncService relationSyncService;
    @Mock
    private ClientStudentRelationRepository relationRepository;

    @Test
    void shouldBuildGroupMembersFromMembershipAndContractSeparately() {
        UUID groupId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Client parent = Client.builder()
                .id(clientId)
                .firstName("Parent")
                .lastName("One")
                .status(ClientStatus.ACTIVE)
                .build();
        Player player = Player.builder()
                .id(playerId)
                .firstName("UX")
                .lastName("Test Child")
                .birthDate(LocalDate.of(2015, 6, 1))
                .parent(parent)
                .build();
        GroupMembership membership = GroupMembership.builder()
                .id(membershipId)
                .groupId(groupId)
                .playerId(playerId)
                .status(GroupMembershipStatus.UPCOMING)
                .joinedAt(LocalDate.now())
                .leftAt(LocalDate.of(2026, 8, 31))
                .build();
        Contract contract = Contract.builder()
                .id(contractId)
                .playerId(playerId)
                .groupId(groupId)
                .contractNumber("CNT-2026-00001")
                .status(ContractStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 6, 24))
                .endDate(LocalDate.of(2026, 8, 31))
                .currency("KZT")
                .build();

        when(groupMembershipPort.findActiveByGroupIdAsOfDate(groupId, LocalDate.now())).thenReturn(List.of(membership));
        when(playerRepository.findByIdIn(any())).thenReturn(List.of(player));
        when(contractRepository.findByPlayerIdIn(any())).thenReturn(List.of(contract));

        ClientService service = new ClientService(
                clientRepository,
                playerRepository,
                contractRepository,
                branchPort,
                authPort,
                groupMembershipPort,
                relationSyncService,
                relationRepository
        );

        List<GroupMemberDto> result = service.getGroupMembers(groupId);

        assertEquals(1, result.size());
        GroupMemberDto item = result.getFirst();
        assertEquals(membershipId, item.membershipId());
        assertEquals(playerId, item.playerId());
        assertEquals("ACTIVE", item.membershipStatus());
        assertEquals(contractId, item.contractId());
        assertEquals("CNT-2026-00001", item.contractNumber());
        assertEquals("ACTIVE", item.contractStatus());
        assertEquals(LocalDate.of(2026, 6, 24), item.contractStartDate());
        assertEquals(LocalDate.of(2026, 8, 31), item.contractEndDate());
    }

    @Test
    void shouldUpdateStudentProfileFields() {
        UUID playerId = UUID.randomUUID();
        Client parent = Client.builder().id(UUID.randomUUID()).branchId(UUID.randomUUID()).build();
        Player player = Player.builder().id(playerId).firstName("Old").lastName("Name").parent(parent).build();
        LocalDate birthDate = LocalDate.of(2014, 3, 12);

        when(playerRepository.findWithParentById(playerId)).thenReturn(java.util.Optional.of(player));
        when(playerRepository.save(player)).thenReturn(player);

        ClientService service = new ClientService(
                clientRepository,
                playerRepository,
                contractRepository,
                branchPort,
                authPort,
                groupMembershipPort,
                relationSyncService,
                relationRepository
        );

        StudentProfileDto result = service.updateStudent(
                playerId,
                new StudentUpdateCommand("New", "Student", birthDate, "Goalkeeper")
        );

        assertEquals("New Student", result.playerFullName());
        assertEquals("New", result.firstName());
        assertEquals("Student", result.lastName());
        assertEquals(birthDate, result.birthDate());
        assertEquals("Goalkeeper", result.position());
        verify(playerRepository).save(player);
    }

    @Test
    void shouldUsePrimaryRelationClientInStudentProfile() {
        UUID playerId = UUID.randomUUID();
        Client legacyParent = Client.builder().id(UUID.randomUUID()).branchId(UUID.randomUUID()).build();
        Client primaryClient = Client.builder()
                .id(UUID.randomUUID())
                .firstName("Primary")
                .lastName("Client")
                .branchId(legacyParent.getBranchId())
                .status(ClientStatus.ACTIVE)
                .build();
        Player player = Player.builder().id(playerId).firstName("Student").lastName("One").parent(legacyParent).build();
        ClientStudentRelation relation = ClientStudentRelation.builder()
                .id(UUID.randomUUID())
                .clientId(primaryClient.getId())
                .playerId(playerId)
                .primaryContact(true)
                .startedAt(LocalDate.now())
                .build();

        when(playerRepository.findWithParentById(playerId)).thenReturn(java.util.Optional.of(player));
        when(relationRepository.findFirstByPlayerIdAndPrimaryContactTrueAndEndedAtIsNullOrderByStartedAtDesc(playerId))
                .thenReturn(java.util.Optional.of(relation));
        when(clientRepository.findById(primaryClient.getId())).thenReturn(java.util.Optional.of(primaryClient));

        ClientService service = new ClientService(
                clientRepository,
                playerRepository,
                contractRepository,
                branchPort,
                authPort,
                groupMembershipPort,
                relationSyncService,
                relationRepository
        );

        StudentProfileDto result = service.getStudentProfile(playerId);

        assertEquals(primaryClient.getId(), result.clientId());
        assertEquals("Primary Client", result.clientFullName());
    }
}
