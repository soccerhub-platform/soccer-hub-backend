package kz.edu.soccerhub.coach.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "training_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSession extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "scheduled_start_at", nullable = false)
    private LocalDateTime scheduledStartAt;

    @Column(name = "scheduled_end_at", nullable = false)
    private LocalDateTime scheduledEndAt;

    @Column(name = "actual_start_at")
    private LocalDateTime actualStartAt;

    @Column(name = "actual_end_at")
    private LocalDateTime actualEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TrainingSessionStatus status;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "topic")
    private String topic;

    @Column(name = "coach_comment")
    private String coachComment;

    @Column(name = "incidents")
    private String incidents;

    @Column(name = "homework")
    private String homework;

    @Column(name = "report_done", nullable = false)
    private boolean reportDone;

    @Column(name = "started_by")
    private UUID startedBy;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
