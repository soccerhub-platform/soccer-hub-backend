package kz.edu.soccerhub.dispacher.application.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record DispatcherBranchesOutput(
        List<DispatcherBranchDto> branches
) {

    @Builder
    public record DispatcherBranchDto(
            UUID branchId,
            String name,
            String address
    ) {}

}