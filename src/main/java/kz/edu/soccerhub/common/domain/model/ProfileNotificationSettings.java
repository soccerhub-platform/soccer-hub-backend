package kz.edu.soccerhub.common.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "profile_notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileNotificationSettings extends AbstractAuditableEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "today_sessions", nullable = false)
    private boolean todaySessions;

    @Column(name = "overdue_reports", nullable = false)
    private boolean overdueReports;

    @Column(name = "schedule_changes", nullable = false)
    private boolean scheduleChanges;

    @Column(name = "lead_reminders", nullable = false)
    private boolean leadReminders;

    @Column(name = "payment_alerts", nullable = false)
    private boolean paymentAlerts;
}
