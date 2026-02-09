package kz.edu.soccerhub.admin.application.dto.coach;

import kz.edu.soccerhub.coach.domain.model.enums.CoachStatus;

public record AdminCoachUpdateCoachStatusInput(
        CoachStatus status
) {
}
