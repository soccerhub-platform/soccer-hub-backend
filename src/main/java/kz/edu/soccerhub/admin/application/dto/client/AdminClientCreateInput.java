package kz.edu.soccerhub.admin.application.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdminClientCreateInput(
        @NotNull UUID branchId,
        @NotBlank String firstName,
        String lastName,
        String phone,
        @Email String email,
        String source,
        String comments
) {
}
