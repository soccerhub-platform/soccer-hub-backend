package kz.edu.soccerhub.coach.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "coach_branches")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoachBranch extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "coach_id")
    private UUID coachId;

    @Column(name = "branch_id")
    private UUID branchId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coach_id", insertable = false, updatable = false)
    private CoachProfile coachProfile;

}
