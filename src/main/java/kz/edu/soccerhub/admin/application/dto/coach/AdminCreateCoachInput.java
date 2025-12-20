package kz.edu.soccerhub.admin.application.dto.coach;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminCreateCoachInput(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String email,
        String phone,
        UUID branchId
) {
}
