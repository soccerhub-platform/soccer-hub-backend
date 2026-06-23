package kz.edu.soccerhub.common.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContractPaymentSummaryOutput(
        UUID contractId,
        BigDecimal contractAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        BigDecimal overpaidAmount,
        ContractPaymentStatus paymentStatus,
        LocalDateTime lastPaidAt,
        int paymentsCount
) {
}
