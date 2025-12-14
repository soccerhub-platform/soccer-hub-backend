package kz.edu.soccerhub.admin.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "admin_branches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class AdminBranch extends AbstractAuditableEntity {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", insertable = false, updatable = false)
    private AdminProfile admin;

}
