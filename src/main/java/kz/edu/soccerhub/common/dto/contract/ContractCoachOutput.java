package kz.edu.soccerhub.common.dto.contract;

import java.util.UUID;

public record ContractCoachOutput(
        UUID id,
        String fullName
) {
}
