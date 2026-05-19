package kz.edu.soccerhub.crm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.crm.domain.model.enums.LeadActivityType;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "lead_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LeadActivity extends AbstractAuditableEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private LeadActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event")
    private LeadEvent event;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private LeadStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private LeadStatus toStatus;

    @Column(name = "assigned_admin_id")
    private UUID assignedAdminId;

    @Column(name = "actor_admin_id")
    private UUID actorAdminId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
}
