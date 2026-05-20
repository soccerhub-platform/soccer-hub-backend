package kz.edu.soccerhub.coach.application.dto.profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CoachProfileResponse(
        UUID coachId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String specialization,
        String bio,
        String status,
        List<CoachBranchItem> branches,
        List<CoachGroupItem> groups,
        LocalDateTime createdAt
) {
}
