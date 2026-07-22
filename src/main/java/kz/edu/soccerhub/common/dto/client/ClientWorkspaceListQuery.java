package kz.edu.soccerhub.common.dto.client;

import java.util.Set;

public record ClientWorkspaceListQuery(
        String search,
        Set<String> statuses,
        String students,
        String contracts,
        String payment
) {
}
