package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminDashboardSeriesPointDto(
        LocalDate date,
        BigDecimal value
) {
}
