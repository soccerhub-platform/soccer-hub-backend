package kz.edu.soccerhub.coach.application.dto.profile;

public record CoachNotificationSettingsResponse(
        boolean todaySessions,
        boolean overdueReports,
        boolean scheduleChanges
) {
}
