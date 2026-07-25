package kz.edu.soccerhub.common.dto.trial;

import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TrialSessionContext(
        UUID sessionId,
        UUID groupId,
        UUID coachId,
        UUID locationId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        TrainingSessionStatus status
) {
}