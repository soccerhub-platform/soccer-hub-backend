package kz.edu.soccerhub.admin.application.dto.group;

import jakarta.validation.constraints.Min;
import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;

import java.util.UUID;

public record AdminGroupUpdateInput(
        String name,
        String description,
        UUID branchId,
        @Min(value = 1, message = "AgeFrom must be at least 1")
        Integer ageFrom,
        @Min(value = 1, message = "AgeTo must be at least 1")
        Integer ageTo,
        GroupAudienceType audienceType,
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,
        GroupLevel level
) {
}
