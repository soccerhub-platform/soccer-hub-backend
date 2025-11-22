package kz.edu.soccerhub.admin.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.UUID;

@Entity
@Table(name = "admin_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class AdminProfileEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    private String firstName;

    private String lastName;

    private String phone;

    private String email;

    private Boolean active;

    @Column(name = "branch_id")
    private UUID branchId;

}
