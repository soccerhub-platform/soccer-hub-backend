package kz.edu.soccerhub.coach.application.dto.profile;

public record CoachNotificationSettingsUpdateRequest(
        boolean todaySessions,
        boolean overdueReports,
        boolean scheduleChanges
) {
}
