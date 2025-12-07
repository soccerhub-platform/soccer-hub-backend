package kz.edu.soccerhub.dispatcher.application.dto.branch;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherBranchCreateOutput(
        UUID branchId
) {}