package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.math.BigDecimal;

public record AdminDashboardKpiItemDto(
        String code,
        String label,
        long value,
        String displayValue,
        AdminDashboardKpiDeltaDto delta,
        String hint,
        String target,
        Long count,
        BigDecimal amount
) {
}
