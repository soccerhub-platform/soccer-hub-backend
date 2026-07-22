package kz.edu.soccerhub.client.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "client_activities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientActivity {

    @Id private UUID id;
    @Column(name = "client_id", nullable = false) private UUID clientId;
    @Enumerated(EnumType.STRING) @Column(name = "activity_type", nullable = false) private ClientActivityType activityType;
    @Column(name = "actor_user_id") private UUID actorUserId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "payload", columnDefinition = "jsonb", nullable = false) private Map<String, Object> payload;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        if (occurredAt == null) occurredAt = now;
        if (createdAt == null) createdAt = now;
    }
}
