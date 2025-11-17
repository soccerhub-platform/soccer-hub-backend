package kz.edu.soccerhub.client.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Client extends AbstractAuditableEntity {

    @Id
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private String firstName;

    private String lastName;

    private String phone;

    private String source;

    @Enumerated(EnumType.STRING)
    private ClientStatus status;

    private String comments;

    @Column(name = "branch_id")
    private UUID branchId;
}
