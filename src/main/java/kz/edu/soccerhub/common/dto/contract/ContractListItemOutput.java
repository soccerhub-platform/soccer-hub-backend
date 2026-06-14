package kz.edu.soccerhub.common.dto.contract;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContractListItemOutput(
        UUID id,
        String contractNumber,
        UUID branchId,
        LeadType leadType,
        ContractStatus status,
        BigDecimal amount,
        String currency,
        LocalDate startDate,
        LocalDate endDate,
        String notes,
        ContractParticipantOutput participant,
        ContractPrimaryContactOutput primaryContact,
        ContractGroupOutput group,
        ContractCoachOutput coach,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
