package kz.edu.soccerhub.coach.application.dto.session;

public record CoachSessionReportView(
        String topic,
        String coachComment,
        String incidents,
        String homework
) {
}
