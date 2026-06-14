package kz.edu.soccerhub.common.dto.contract;

import java.time.LocalDate;
import java.util.UUID;

public record ContractParticipantOutput(
        UUID id,
        String fullName,
        LocalDate birthDate
) {
}
