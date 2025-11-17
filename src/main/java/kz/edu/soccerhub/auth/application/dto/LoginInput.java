package kz.edu.soccerhub.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.edu.soccerhub.common.domain.enums.Role;

public record LoginInput(
        @Email(message = "")
        String email,
        @NotBlank(message = "Password must be non-blank")
        @Size(min = 6, message = "Password must have at least 6 characters")
        String password,
        Role role
) {}