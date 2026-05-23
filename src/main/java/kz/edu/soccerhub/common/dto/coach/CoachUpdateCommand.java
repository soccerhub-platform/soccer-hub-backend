package kz.edu.soccerhub.common.dto.coach;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CoachUpdateCommand(
        UUID coachId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String specialization
) {
}
