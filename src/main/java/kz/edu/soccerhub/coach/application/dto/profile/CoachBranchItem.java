package kz.edu.soccerhub.coach.application.dto.profile;

import java.util.UUID;

public record CoachBranchItem(
        UUID branchId,
        String branchName
) {
}
