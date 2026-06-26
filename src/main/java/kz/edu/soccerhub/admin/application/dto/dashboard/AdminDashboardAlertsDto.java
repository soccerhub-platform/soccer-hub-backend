package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.List;

public record AdminDashboardAlertsDto(
        List<AdminDashboardAlertCardDto> topCards,
        List<AdminDashboardAttentionItemDto> attention
) {
}
