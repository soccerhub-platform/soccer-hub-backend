package kz.edu.soccerhub.common.dto.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractUpdateCommand(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal amount,
        String currency,
        String notes
) {
}
