package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.List;

public record AdminDashboardLeadFunnelDto(
        List<AdminDashboardLeadFunnelRowDto> rows,
        int conversionToClientPercent
) {
}
