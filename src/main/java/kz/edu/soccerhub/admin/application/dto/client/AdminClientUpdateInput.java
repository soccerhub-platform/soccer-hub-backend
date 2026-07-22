package kz.edu.soccerhub.admin.application.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminClientUpdateInput(
        @NotBlank String firstName,
        String lastName,
        String phone,
        @Email String email,
        String source,
        String comments
) {
}
