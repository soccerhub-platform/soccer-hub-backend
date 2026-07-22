package kz.edu.soccerhub.client.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "client_profiles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Client extends AbstractAuditableEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    private String firstName;

    private String lastName;

    private String phone;

    private String email;

    private String source;

    @Enumerated(EnumType.STRING)
    private ClientStatus status;

    private String comments;

    @Column(name = "branch_id")
    private UUID branchId;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        isNew = false;
    }
}
