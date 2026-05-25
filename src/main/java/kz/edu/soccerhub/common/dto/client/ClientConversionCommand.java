package kz.edu.soccerhub.common.dto.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ClientConversionCommand(
        UUID existingClientId,
        String parentName,
        String phone,
        String email,
        UUID branchId,
        String source,
        String comments,
        String childName,
        LocalDate childBirthDate,
        UUID groupId,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        BigDecimal amount
) {
}
