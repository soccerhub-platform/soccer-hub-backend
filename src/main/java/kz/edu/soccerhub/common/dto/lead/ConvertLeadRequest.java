package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConvertLeadRequest(
        @NotNull(message = "participantId is required")
        UUID participantId,

        @NotNull(message = "groupId is required")
        UUID groupId,

        @NotNull(message = "participantBirthDate is required")
        LocalDate participantBirthDate,

        @NotNull(message = "contractStartDate is required")
        LocalDate contractStartDate,

        LocalDate contractEndDate,

        @DecimalMin(value = "0", message = "amount must be >= 0")
        BigDecimal amount
) {
}
