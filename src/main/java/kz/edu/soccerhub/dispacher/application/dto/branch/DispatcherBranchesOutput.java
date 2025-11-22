package kz.edu.soccerhub.dispacher.application.dto.branch;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherBranchesOutput(
        UUID branchId,
        String name,
        String address,
        UUID clubId
) {

}