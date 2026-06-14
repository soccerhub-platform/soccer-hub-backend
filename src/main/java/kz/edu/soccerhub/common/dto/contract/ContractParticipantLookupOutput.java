package kz.edu.soccerhub.common.dto.contract;

import kz.edu.soccerhub.crm.domain.model.enums.LeadType;

import java.time.LocalDate;
import java.util.UUID;

public record ContractParticipantLookupOutput(
        UUID id,
        String fullName,
        LocalDate birthDate,
        LeadType leadType,
        ContractPrimaryContactOutput primaryContact
) {
}
