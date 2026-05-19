package kz.edu.soccerhub.coach.application.dto.session;

import java.util.UUID;

public record CoachSessionStudentItem(
        UUID id,
        String name,
        String attendance
) {
}
