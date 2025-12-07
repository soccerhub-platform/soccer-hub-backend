package kz.edu.soccerhub.organization.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "group_coaches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCoach extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private CoachRole role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "assigned_from")
    private LocalDate assignedFrom;

    @Column(name = "assigned_to")
    private LocalDate assignedTo;

}
