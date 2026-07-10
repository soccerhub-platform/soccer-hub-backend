package kz.edu.soccerhub.admin.application.dto.coach;

import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatus;
import kz.edu.soccerhub.coach.domain.model.enums.WorkStatus;

import java.time.LocalDate;

public record AdminCoachUpdateCoachStatusInput(
        AccountStatus accountStatus,
        WorkStatus workStatus,
        LocalDate vacationFrom,
        LocalDate vacationTo,
        String reason,
        CoachStatus status
) {
}
