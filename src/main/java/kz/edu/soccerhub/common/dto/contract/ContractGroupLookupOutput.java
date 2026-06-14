package kz.edu.soccerhub.common.dto.contract;

import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;

import java.util.UUID;

public record ContractGroupLookupOutput(
        UUID id,
        UUID branchId,
        String name,
        GroupAudienceType audienceType,
        ContractCoachOutput coach
) {
}
