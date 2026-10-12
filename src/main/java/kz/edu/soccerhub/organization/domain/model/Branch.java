package kz.edu.soccerhub.organization.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;

import java.util.UUID;
import java.util.List;
import java.time.Instant;
import kz.edu.soccerhub.common.dto.branch.BranchWorkingDay;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "branches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "club_id")
    private UUID clubId;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 20)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(name = "manager_admin_id")
    private UUID managerAdminId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "working_hours", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<BranchWorkingDay> workingHours = List.of();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne
    @JoinColumn(name = "club_id", insertable = false, updatable = false)
    private Club club;

}