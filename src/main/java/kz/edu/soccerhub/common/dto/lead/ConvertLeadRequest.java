package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConvertLeadRequest(
        @NotNull(message = "childId is required")
        UUID childId,

        @NotNull(message = "groupId is required")
        UUID groupId,

        @NotNull(message = "childBirthDate is required")
        LocalDate childBirthDate,

        @NotNull(message = "contractStartDate is required")
        LocalDate contractStartDate,

        LocalDate contractEndDate,

        @DecimalMin(value = "0", message = "amount must be >= 0")
        BigDecimal amount
) {
}
