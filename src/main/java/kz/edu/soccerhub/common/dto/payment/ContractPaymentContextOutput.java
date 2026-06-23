package kz.edu.soccerhub.common.dto.payment;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ContractPaymentContextOutput(
        UUID contractId,
        String contractNumber,
        UUID clientId,
        String clientName,
        UUID playerId,
        String playerName,
        UUID branchId,
        BigDecimal contractAmount,
        String currency,
        ContractStatus contractStatus
) {
}
