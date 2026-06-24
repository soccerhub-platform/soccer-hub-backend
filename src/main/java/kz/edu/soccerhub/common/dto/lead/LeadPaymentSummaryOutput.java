package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeadPaymentSummaryOutput(
        ContractPaymentStatus paymentStatus,
        BigDecimal contractAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        LocalDateTime lastPaidAt
) {
}
