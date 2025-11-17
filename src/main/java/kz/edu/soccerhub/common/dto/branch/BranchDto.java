package kz.edu.soccerhub.common.dto.branch;

import lombok.Builder;

import java.util.UUID;

@Builder
public record BranchDto(
        UUID id,
        String name,
        String address,
        boolean active
) {
}
