package kz.edu.soccerhub.common.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record PaymentCancelCommand(
        @NotBlank(message = "reason is required")
        String reason,
        String comment
) {
}
