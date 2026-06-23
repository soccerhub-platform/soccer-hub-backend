package kz.edu.soccerhub.common.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record ContractPaymentSummaryQueryInput(
        UUID contractId,
        BigDecimal contractAmount
) {
}
