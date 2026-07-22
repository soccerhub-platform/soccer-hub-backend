package kz.edu.soccerhub.common.dto.contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractCreateCommand(
        UUID branchId,
        UUID clientId,
        UUID playerId,
        String contractNumber,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal amount,
        String currency,
        String notes
) {
}
