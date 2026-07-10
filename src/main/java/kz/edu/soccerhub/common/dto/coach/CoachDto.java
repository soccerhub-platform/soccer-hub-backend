package kz.edu.soccerhub.common.dto.coach;

import lombok.Builder;
import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.enums.WorkStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CoachDto(
        UUID id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String phone,
        String email,
        String specialization,
        boolean active,
        AccountStatus accountStatus,
        WorkStatus workStatus,
        LocalDate vacationFrom,
        LocalDate vacationTo,
        String workStatusReason,
        LocalDateTime createdAt
) {
}
