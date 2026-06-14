package kz.edu.soccerhub.common.dto.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractExtendCommand(
        LocalDate endDate,
        BigDecimal amount,
        String notes
) {
}
