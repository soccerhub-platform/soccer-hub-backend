package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.ClientStudentRelation;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientStudentRelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ClientStudentRelationSyncService {

    private final ClientStudentRelationRepository relationRepository;

    @Transactional
    public void syncLegacyParent(Player player) {
        Client parent = player.getParent();
        if (parent == null || player.getId() == null
                || relationRepository.existsByClientIdAndPlayerIdAndEndedAtIsNull(parent.getId(), player.getId())) {
            return;
        }

        relationRepository.save(ClientStudentRelation.builder()
                .clientId(parent.getId())
                .playerId(player.getId())
                .relationshipType(ClientStudentRelationshipType.LEGACY_PARENT)
                .primaryContact(true)
                .primaryPayer(true)
                .legalRepresentative(true)
                .receivesNotifications(true)
                .startedAt(player.getCreatedAt() == null ? LocalDate.now() : player.getCreatedAt().toLocalDate())
                .build());
    }
}
