package kz.edu.soccerhub.coach.application.dto.profile;

import java.util.UUID;

public record CoachGroupItem(
        UUID groupId,
        String groupName,
        UUID branchId,
        String branchName,
        String role
) {
}
