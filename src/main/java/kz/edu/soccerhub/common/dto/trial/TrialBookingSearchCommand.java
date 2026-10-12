package kz.edu.soccerhub.common.dto.trial;

import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TrialBookingSearchCommand(
        TrialBookingStatus status,
        UUID leadId,
        UUID clientId,
        UUID studentId,
        UUID trainingSessionId, java.util.Set<UUID> sessionIds
) {
 public TrialBookingSearchCommand(TrialBookingStatus status,UUID leadId,UUID clientId,UUID studentId,UUID trainingSessionId) {this(status,leadId,clientId,studentId,trainingSessionId,null);}
}
