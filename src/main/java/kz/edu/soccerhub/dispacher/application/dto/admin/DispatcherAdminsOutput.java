package kz.edu.soccerhub.dispacher.application.dto.admin;

import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record DispatcherAdminsOutput(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean isActive,
        Set<BranchWithClub> branches

) {

    @Builder
    public record BranchWithClub(
            UUID branchId,
            String branchName,
            UUID clubId,
            String clubName
    ) {}
}
