package kz.edu.soccerhub.admin.application.dto.dashboard;

import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

import java.util.Map;

public record AdminDashboardLeadFunnelDto(
        Map<LeadStatus, Long> totals
) {
}
