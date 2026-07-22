package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.ClientStudentRelation;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientStudentRelationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientStudentRelationSyncServiceTest {

    @Mock
    private ClientStudentRelationRepository relationRepository;

    @Test
    void shouldCreateLegacyRelationForExistingParentLink() {
        Client client = Client.builder().id(UUID.randomUUID()).build();
        Player player = Player.builder().id(UUID.randomUUID()).parent(client).build();
        ClientStudentRelationSyncService service = new ClientStudentRelationSyncService(relationRepository);

        service.syncLegacyParent(player);

        ArgumentCaptor<ClientStudentRelation> captor = ArgumentCaptor.forClass(ClientStudentRelation.class);
        verify(relationRepository).save(captor.capture());
        ClientStudentRelation relation = captor.getValue();
        assertEquals(client.getId(), relation.getClientId());
        assertEquals(player.getId(), relation.getPlayerId());
        assertEquals(ClientStudentRelationshipType.LEGACY_PARENT, relation.getRelationshipType());
        assertTrue(relation.isPrimaryContact());
        assertTrue(relation.isPrimaryPayer());
        assertTrue(relation.isLegalRepresentative());
        assertTrue(relation.isReceivesNotifications());
        assertEquals(LocalDate.now(), relation.getStartedAt());
    }

    @Test
    void shouldNotDuplicateActiveRelation() {
        Client client = Client.builder().id(UUID.randomUUID()).build();
        Player player = Player.builder().id(UUID.randomUUID()).parent(client).build();
        when(relationRepository.existsByClientIdAndPlayerIdAndEndedAtIsNull(client.getId(), player.getId()))
                .thenReturn(true);
        ClientStudentRelationSyncService service = new ClientStudentRelationSyncService(relationRepository);

        service.syncLegacyParent(player);

        verify(relationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
