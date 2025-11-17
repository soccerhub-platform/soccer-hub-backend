package kz.edu.soccerhub.common.dto.branch;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateBranchCommand(
        UUID clubId,
        String name,
        String address
) {}