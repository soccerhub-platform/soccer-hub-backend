package kz.edu.soccerhub.common.dto.client;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ClientCreateCommand(
        String firstName,
        String lastName,
        String phone,
        UUID branchId,
        String source,
        String comments
) {
}
