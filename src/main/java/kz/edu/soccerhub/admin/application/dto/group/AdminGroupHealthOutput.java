package kz.edu.soccerhub.admin.application.dto.group;

import java.util.List;
import java.util.UUID;

public record AdminGroupHealthOutput(
        UUID groupId,
        GroupHealth health,
        List<IssueItem> issues,
        List<String> recommendedActions
) {
    public record IssueItem(
            GroupIssueCode code,
            String message
    ) {}
}
