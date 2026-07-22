package kz.edu.soccerhub.client.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "client_student_relations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientStudentRelation extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    private ClientStudentRelationshipType relationshipType;

    @Column(name = "is_primary_contact", nullable = false)
    private boolean primaryContact;

    @Column(name = "is_primary_payer", nullable = false)
    private boolean primaryPayer;

    @Column(name = "is_legal_representative", nullable = false)
    private boolean legalRepresentative;

    @Column(name = "receives_notifications", nullable = false)
    private boolean receivesNotifications;

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "ended_at")
    private LocalDate endedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
