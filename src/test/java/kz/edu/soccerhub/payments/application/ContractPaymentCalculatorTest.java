package kz.edu.soccerhub.payments.application;

import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import kz.edu.soccerhub.payments.domain.model.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContractPaymentCalculatorTest {

    private final ContractPaymentCalculator calculator = new ContractPaymentCalculator();

    @Test
    void shouldCalculatePartialSummaryIgnoringCancelledPayments() {
        UUID contractId = UUID.randomUUID();

        ContractPaymentSummaryOutput summary = calculator.summarize(
                contractId,
                BigDecimal.valueOf(80000),
                List.of(
                        payment(BigDecimal.valueOf(50000), PaymentStatus.PAID, LocalDateTime.of(2026, 6, 23, 12, 0)),
                        payment(BigDecimal.valueOf(10000), PaymentStatus.CANCELLED, LocalDateTime.of(2026, 6, 24, 12, 0))
                )
        );

        assertEquals(ContractPaymentStatus.PARTIALLY_PAID, summary.paymentStatus());
        assertEquals(BigDecimal.valueOf(50000), summary.paidAmount());
        assertEquals(BigDecimal.valueOf(30000), summary.outstandingAmount());
        assertEquals(BigDecimal.ZERO, summary.overpaidAmount());
        assertEquals(1, summary.paymentsCount());
        assertEquals(LocalDateTime.of(2026, 6, 23, 12, 0), summary.lastPaidAt());
    }

    @Test
    void shouldCalculatePaidWithOverpayment() {
        UUID contractId = UUID.randomUUID();

        ContractPaymentSummaryOutput summary = calculator.summarize(
                contractId,
                BigDecimal.valueOf(80000),
                List.of(payment(BigDecimal.valueOf(100000), PaymentStatus.PAID, LocalDateTime.of(2026, 6, 23, 12, 0)))
        );

        assertEquals(ContractPaymentStatus.PAID, summary.paymentStatus());
        assertEquals(BigDecimal.valueOf(100000), summary.paidAmount());
        assertEquals(BigDecimal.ZERO, summary.outstandingAmount());
        assertEquals(BigDecimal.valueOf(20000), summary.overpaidAmount());
    }

    @Test
    void shouldReturnZeroStateWhenNoPaymentsExist() {
        UUID contractId = UUID.randomUUID();

        ContractPaymentSummaryOutput summary = calculator.summarize(
                contractId,
                BigDecimal.valueOf(80000),
                List.of()
        );

        assertEquals(ContractPaymentStatus.UNPAID, summary.paymentStatus());
        assertEquals(BigDecimal.ZERO, summary.paidAmount());
        assertEquals(BigDecimal.valueOf(80000), summary.outstandingAmount());
        assertEquals(BigDecimal.ZERO, summary.overpaidAmount());
        assertEquals(0, summary.paymentsCount());
        assertNull(summary.lastPaidAt());
    }

    @Test
    void shouldCalculateFullyPaidWithoutOverpayment() {
        UUID contractId = UUID.randomUUID();

        ContractPaymentSummaryOutput summary = calculator.summarize(
                contractId,
                BigDecimal.valueOf(80000),
                List.of(payment(BigDecimal.valueOf(80000), PaymentStatus.PAID, LocalDateTime.of(2026, 6, 23, 12, 0)))
        );

        assertEquals(ContractPaymentStatus.PAID, summary.paymentStatus());
        assertEquals(BigDecimal.valueOf(80000), summary.paidAmount());
        assertEquals(BigDecimal.ZERO, summary.outstandingAmount());
        assertEquals(BigDecimal.ZERO, summary.overpaidAmount());
    }

    private Payment payment(BigDecimal amount, PaymentStatus status, LocalDateTime paidAt) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .contractId(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .branchId(UUID.randomUUID())
                .amount(amount)
                .currency("KZT")
                .status(status)
                .method(PaymentMethod.KASPI)
                .paidAt(paidAt)
                .recordedAt(paidAt)
                .recordedBy(UUID.randomUUID())
                .build();
    }
}
