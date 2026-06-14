package kz.edu.soccerhub.common.dto.contract;

import java.time.LocalDate;

public record ContractParticipantDraftInput(
        String fullName,
        LocalDate birthDate
) {
}
