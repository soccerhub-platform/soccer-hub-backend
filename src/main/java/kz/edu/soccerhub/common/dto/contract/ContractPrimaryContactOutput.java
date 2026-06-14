package kz.edu.soccerhub.common.dto.contract;

import java.util.UUID;

public record ContractPrimaryContactOutput(
        UUID id,
        String fullName,
        String phone,
        String email
) {
}
