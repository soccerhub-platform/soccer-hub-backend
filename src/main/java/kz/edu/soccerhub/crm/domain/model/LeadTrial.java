package kz.edu.soccerhub.crm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.crm.domain.model.enums.LeadTrialStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "lead_trials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LeadTrial extends AbstractAuditableEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false, unique = true)
    private Lead lead;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "coach_id")
    private UUID coachId;

    @Column(name = "trial_date", nullable = false)
    private LocalDate trialDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadTrialStatus status;

    public void attachToLead(Lead lead) {
        this.lead = lead;
    }

    public void schedule(
            UUID childId,
            UUID groupId,
            UUID coachId,
            LocalDate trialDate,
            LocalTime startTime,
            LocalTime endTime,
            String comment
    ) {
        this.childId = childId;
        this.groupId = groupId;
        this.coachId = coachId;
        this.trialDate = trialDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.comment = comment;
        this.status = LeadTrialStatus.SCHEDULED;
    }

    @PrePersist
    private void ensureDefaults() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = LeadTrialStatus.SCHEDULED;
        }
    }
}

