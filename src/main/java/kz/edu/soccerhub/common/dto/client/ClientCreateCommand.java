package kz.edu.soccerhub.common.dto.client;

import kz.edu.soccerhub.client.domain.enums.ClientSource;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ClientCreateCommand(
        String firstName,
        String lastName,
        String phone,
        UUID branchId,
        ClientSource source,
        String sourceDetails,
        String comments
) {
}
