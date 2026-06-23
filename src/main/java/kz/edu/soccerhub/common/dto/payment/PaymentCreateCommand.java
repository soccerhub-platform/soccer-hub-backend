package kz.edu.soccerhub.common.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCreateCommand(
        @NotNull(message = "contractId is required")
        UUID contractId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be > 0")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency,

        @NotNull(message = "method is required")
        PaymentMethod method,

        @NotNull(message = "paidAt is required")
        LocalDateTime paidAt,

        String comment,

        String externalReference
) {
}
