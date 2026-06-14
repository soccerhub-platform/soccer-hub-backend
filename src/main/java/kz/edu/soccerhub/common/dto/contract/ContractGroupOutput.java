package kz.edu.soccerhub.common.dto.contract;

import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;

import java.util.UUID;

public record ContractGroupOutput(
        UUID id,
        String name,
        GroupAudienceType audienceType
) {
}
