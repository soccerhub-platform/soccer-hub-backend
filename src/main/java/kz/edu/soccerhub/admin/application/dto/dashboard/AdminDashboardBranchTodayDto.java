package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardBranchTodayDto(
        int trainersOnDuty,
        int groupsWithoutCoach,
        int groupsWithoutSchedule,
        int avgFirstResponseMinutes
) {
}
