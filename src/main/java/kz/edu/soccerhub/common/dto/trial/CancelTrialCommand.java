package kz.edu.soccerhub.common.dto.trial;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CancelTrialCommand(UUID trialId, String reason, UUID adminId,
        Long version
) {
    public CancelTrialCommand(UUID trialId, String reason, UUID adminId) { this(trialId,reason,adminId, null); }
}
