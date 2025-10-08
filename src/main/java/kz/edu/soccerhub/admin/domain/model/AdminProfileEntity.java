package kz.edu.soccerhub.admin.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.common.domain.model.BranchEntity;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "admin_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileEntity extends AbstractAuditableEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    private String firstName;

    private String lastName;

    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

}
