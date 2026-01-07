package kz.edu.soccerhub.admin.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminGroupCreateInput(
        @NotNull
        String name,
        String description,
        @NotNull
        UUID branchId,
        @Min(value = 1, message = "AgeFrom must be at least 1")
        Integer ageFrom,
        @Min(value = 1, message = "AgeTo must be at least 1")
        Integer ageTo,
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,
        @NotNull
        GroupLevel level
) {
}
