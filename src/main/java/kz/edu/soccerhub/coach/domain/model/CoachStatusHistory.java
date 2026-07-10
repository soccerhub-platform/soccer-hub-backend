package kz.edu.soccerhub.coach.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatus;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatusHistoryEventType;
import kz.edu.soccerhub.coach.domain.model.enums.WorkStatus;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coach_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachStatusHistory extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CoachStatus status;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50)
    private CoachStatusHistoryEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_account_status", length = 30)
    private AccountStatus previousAccountStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_account_status", length = 30)
    private AccountStatus newAccountStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_work_status", length = 30)
    private WorkStatus previousWorkStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_work_status", length = 30)
    private WorkStatus newWorkStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "vacation_from")
    private LocalDate vacationFrom;

    @Column(name = "vacation_to")
    private LocalDate vacationTo;
}
