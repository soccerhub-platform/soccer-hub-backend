package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.ClientStudentRelation;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ClientStudentRelationRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationEndCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientStudentRelationServiceTest {

    @Mock private ClientStudentRelationRepository relationRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private PlayerRepository playerRepository;

    private ClientStudentRelationService service;

    @BeforeEach
    void setUp() {
        service = new ClientStudentRelationService(relationRepository, clientRepository, playerRepository);
    }

    @Test
    void createShouldTransferPrimaryRolesAndSyncLegacyParent() {
        UUID branchId = UUID.randomUUID();
        Client oldClient = client(branchId);
        Client newClient = client(branchId);
        Player player = player(oldClient);
        ClientStudentRelation oldRelation = relation(oldClient, player, ClientStudentRelationshipType.MOTHER, true, true);
        ClientStudentRelationCreateCommand command = new ClientStudentRelationCreateCommand(
                newClient.getId(), player.getId(), ClientStudentRelationshipType.FATHER,
                true, true, true, true, true, true, LocalDate.now()
        );

        when(clientRepository.findById(newClient.getId())).thenReturn(Optional.of(newClient));
        when(playerRepository.findWithParentById(player.getId())).thenReturn(Optional.of(player));
        when(relationRepository.findByPlayerIdAndEndedAtIsNull(player.getId())).thenReturn(List.of(oldRelation));
        when(relationRepository.save(any())).thenAnswer(invocation -> {
            ClientStudentRelation saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        service.create(command);

        assertFalse(oldRelation.isPrimaryContact());
        assertFalse(oldRelation.isPrimaryPayer());
        assertEquals(newClient, player.getParent());
        verify(playerRepository).save(player);
        ArgumentCaptor<ClientStudentRelation> relation = ArgumentCaptor.forClass(ClientStudentRelation.class);
        verify(relationRepository).save(relation.capture());
        assertEquals(ClientStudentRelationshipType.FATHER, relation.getValue().getRelationshipType());
    }

    @Test
    void createShouldRejectSecondSelf() {
        UUID branchId = UUID.randomUUID();
        Client oldClient = client(branchId);
        Client newClient = client(branchId);
        Player player = player(oldClient);
        when(clientRepository.findById(newClient.getId())).thenReturn(Optional.of(newClient));
        when(playerRepository.findWithParentById(player.getId())).thenReturn(Optional.of(player));
        when(relationRepository.findByPlayerIdAndEndedAtIsNull(player.getId()))
                .thenReturn(List.of(relation(oldClient, player, ClientStudentRelationshipType.SELF, true, true)));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.create(
                new ClientStudentRelationCreateCommand(
                        newClient.getId(), player.getId(), ClientStudentRelationshipType.SELF,
                        true, true, true, true, true, true, LocalDate.now()
                )
        ));

        assertEquals("CLIENT_STUDENT_SELF_EXISTS", exception.getErrorCode());
        verify(relationRepository, never()).save(any());
    }

    @Test
    void createShouldRequireExplicitPrimaryRoleReplacement() {
        UUID branchId = UUID.randomUUID();
        Client oldClient = client(branchId);
        Client newClient = client(branchId);
        Player player = player(oldClient);
        ClientStudentRelation oldRelation = relation(oldClient, player, ClientStudentRelationshipType.MOTHER, true, true);

        when(clientRepository.findById(newClient.getId())).thenReturn(Optional.of(newClient));
        when(playerRepository.findWithParentById(player.getId())).thenReturn(Optional.of(player));
        when(relationRepository.findByPlayerIdAndEndedAtIsNull(player.getId())).thenReturn(List.of(oldRelation));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.create(
                new ClientStudentRelationCreateCommand(
                        newClient.getId(), player.getId(), ClientStudentRelationshipType.FATHER,
                        true, true, false, false, true, true, LocalDate.now()
                )
        ));

        assertEquals("CLIENT_STUDENT_PRIMARY_CONTACT_REPLACEMENT_REQUIRED", exception.getErrorCode());
        verify(relationRepository, never()).save(any());
    }

    @Test
    void endShouldRejectLastRelation() {
        Client client = client(UUID.randomUUID());
        Player player = player(client);
        ClientStudentRelation relation = relation(client, player, ClientStudentRelationshipType.MOTHER, false, false);
        when(relationRepository.findLockedById(relation.getId())).thenReturn(Optional.of(relation));
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        when(playerRepository.findWithParentById(player.getId())).thenReturn(Optional.of(player));
        when(relationRepository.findByPlayerIdAndEndedAtIsNull(player.getId())).thenReturn(List.of(relation));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.end(
                new ClientStudentRelationEndCommand(relation.getId(), LocalDate.now())
        ));
        assertEquals("CLIENT_STUDENT_LAST_RELATION", exception.getErrorCode());
    }

    @Test
    void createStudentShouldPersistPlayerAndStandardRelation() {
        Client client = client(UUID.randomUUID());
        LocalDate birthDate = LocalDate.of(2014, 4, 12);
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        when(playerRepository.findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(
                client.getId(), "Ayan", "Test", birthDate)).thenReturn(Optional.empty());
        when(playerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerRepository.findWithParentById(any())).thenAnswer(invocation -> {
            UUID playerId = invocation.getArgument(0);
            return Optional.of(Player.builder().id(playerId).firstName("Ayan").lastName("Test")
                    .birthDate(birthDate).parent(client).build());
        });
        when(relationRepository.findByPlayerIdAndEndedAtIsNull(any())).thenReturn(List.of());
        when(relationRepository.save(any())).thenAnswer(invocation -> {
            ClientStudentRelation saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var output = service.createStudent(new ClientStudentCreateCommand(
                client.getId(), " Ayan ", " Test ", birthDate, ClientStudentRelationshipType.MOTHER,
                true, true, true, true, LocalDate.now()
        ));

        assertEquals("Ayan Test", output.playerName());
        ArgumentCaptor<Player> player = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(player.capture());
        assertEquals(client, player.getValue().getParent());
        assertEquals(birthDate, player.getValue().getBirthDate());
        verify(relationRepository).save(any(ClientStudentRelation.class));
    }

    private Client client(UUID branchId) {
        return Client.builder().id(UUID.randomUUID()).firstName("Client").lastName("Name").branchId(branchId).build();
    }

    private Player player(Client parent) {
        return Player.builder().id(UUID.randomUUID()).firstName("Student").lastName("Name").parent(parent).build();
    }

    private ClientStudentRelation relation(
            Client client, Player player, ClientStudentRelationshipType type, boolean primaryContact, boolean primaryPayer
    ) {
        return ClientStudentRelation.builder()
                .id(UUID.randomUUID()).clientId(client.getId()).playerId(player.getId()).relationshipType(type)
                .primaryContact(primaryContact).primaryPayer(primaryPayer).legalRepresentative(true)
                .receivesNotifications(true).startedAt(LocalDate.now().minusMonths(1)).build();
    }
}
