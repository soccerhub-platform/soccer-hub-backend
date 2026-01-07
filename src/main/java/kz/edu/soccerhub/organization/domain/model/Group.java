package kz.edu.soccerhub.organization.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "age_from")
    private Integer ageFrom;

    @Column(name = "age_to")
    private Integer ageTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    private GroupLevel level;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private GroupStatus status;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Branch branch;
}
