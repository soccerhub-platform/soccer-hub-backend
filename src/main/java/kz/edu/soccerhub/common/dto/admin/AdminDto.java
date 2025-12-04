package kz.edu.soccerhub.common.dto.admin;

import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record AdminDto(

        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean isActive,
        UUID dispatcherId,
        Set<UUID> branchesId
) {
}
