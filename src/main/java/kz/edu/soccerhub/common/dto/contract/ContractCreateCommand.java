package kz.edu.soccerhub.common.dto.contract;

import kz.edu.soccerhub.crm.domain.model.enums.LeadType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractCreateCommand(
        UUID branchId,
        LeadType leadType,
        UUID participantId,
        ContractParticipantDraftInput participantDraft,
        UUID primaryContactId,
        ContractPrimaryContactDraftInput primaryContactDraft,
        UUID groupId,
        UUID coachId,
        String contractNumber,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal amount,
        String currency,
        String notes
) {
}
