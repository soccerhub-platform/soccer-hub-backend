package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record LeadContractSummaryOutput(
        UUID id,
        String contractNumber,
        ContractStatus status,
        BigDecimal amount,
        String currency
) {
}
