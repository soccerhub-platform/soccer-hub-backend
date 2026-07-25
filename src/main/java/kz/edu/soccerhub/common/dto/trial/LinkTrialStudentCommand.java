package kz.edu.soccerhub.common.dto.trial;

import lombok.Builder;

import java.util.UUID;

@Builder
public record LinkTrialStudentCommand(
        UUID leadId,
        UUID participantId,
        UUID clientId,
        UUID studentId
) {
}
