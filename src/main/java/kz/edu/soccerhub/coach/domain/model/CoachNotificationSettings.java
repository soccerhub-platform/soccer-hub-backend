package kz.edu.soccerhub.coach.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "coach_notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachNotificationSettings extends AbstractAuditableEntity {

    @Id
    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Builder.Default
    @Column(name = "today_sessions", nullable = false)
    private boolean todaySessions = true;

    @Builder.Default
    @Column(name = "overdue_reports", nullable = false)
    private boolean overdueReports = true;

    @Builder.Default
    @Column(name = "schedule_changes", nullable = false)
    private boolean scheduleChanges = true;
}
