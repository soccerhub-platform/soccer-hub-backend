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

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "coach_availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachAvailability extends AbstractAuditableEntity {

    @Id
    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "days", nullable = false)
    private String days;

    @Column(name = "time_from", nullable = false)
    private LocalTime timeFrom;

    @Column(name = "time_to", nullable = false)
    private LocalTime timeTo;

    @Column(name = "timezone", nullable = false)
    private String timezone;
}
