package kz.edu.soccerhub.payments.application;

import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import kz.edu.soccerhub.payments.domain.model.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class ContractPaymentCalculator {

    public ContractPaymentSummaryOutput summarize(UUID contractId, BigDecimal contractAmount, List<Payment> payments) {
        BigDecimal normalizedContractAmount = normalize(contractAmount);
        BigDecimal paidAmount = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingAmount = normalizedContractAmount.subtract(paidAmount).max(BigDecimal.ZERO);
        BigDecimal overpaidAmount = paidAmount.subtract(normalizedContractAmount).max(BigDecimal.ZERO);

        ContractPaymentStatus status;
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            status = ContractPaymentStatus.UNPAID;
        } else if (paidAmount.compareTo(normalizedContractAmount) < 0) {
            status = ContractPaymentStatus.PARTIALLY_PAID;
        } else {
            status = ContractPaymentStatus.PAID;
        }

        LocalDateTime lastPaidAt = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(Payment::getPaidAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        int paymentsCount = (int) payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .count();

        return new ContractPaymentSummaryOutput(
                contractId,
                normalizedContractAmount,
                paidAmount,
                outstandingAmount,
                overpaidAmount,
                status,
                lastPaidAt,
                paymentsCount
        );
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
