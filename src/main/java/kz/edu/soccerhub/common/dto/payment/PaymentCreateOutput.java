package kz.edu.soccerhub.common.dto.payment;

import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreateOutput(
        UUID paymentId,
        UUID contractId,
        PaymentStatus paymentStatus,
        ContractPaymentStatus contractPaymentStatus,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        BigDecimal overpaidAmount
) {
}
