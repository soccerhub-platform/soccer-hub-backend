package kz.edu.soccerhub.common.dto.profile;

public record NotificationSettings(
        boolean todaySessions,
        boolean overdueReports,
        boolean scheduleChanges,
        boolean leadReminders,
        boolean paymentAlerts
) {
}
