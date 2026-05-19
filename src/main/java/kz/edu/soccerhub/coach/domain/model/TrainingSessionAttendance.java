package kz.edu.soccerhub.coach.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "training_session_attendance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSessionAttendance extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TrainingSessionAttendanceStatus status;

    @Column(name = "comment")
    private String comment;

    @Column(name = "marked_by")
    private UUID markedBy;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt;
}
