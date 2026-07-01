package kz.edu.soccerhub.admin.application.dto.dashboard;

import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

public record AdminDashboardLeadFunnelRowDto(
        LeadStatus status,
        String label,
        long count,
        int percent
) {
}
