package kz.edu.soccerhub.coach.application.dto.session;

public record CoachAttendanceUpdateResponse(
        boolean ok,
        String attendanceSummary
) {
}
