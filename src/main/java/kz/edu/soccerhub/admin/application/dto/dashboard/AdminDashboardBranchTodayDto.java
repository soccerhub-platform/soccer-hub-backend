package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.Map;

public record AdminDashboardBranchTodayDto(
        Long studentsTotal,
        Long studentsDelta,
        Long trainingsVisited,
        Long trainingsTotal,
        Integer attendancePercent,
        Long newStudents,
        Long newStudentsDelta,
        Integer trainersOnDuty,
        Integer groupsWithoutCoach,
        Integer groupsWithoutSchedule,
        Integer avgFirstResponseMinutes,
        Map<String, String> unavailableReasons
) {
}
