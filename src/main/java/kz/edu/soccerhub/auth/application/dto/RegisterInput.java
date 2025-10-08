package kz.edu.soccerhub.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.domain.enums.Role;

import java.util.Set;

public record RegisterInput(
        @Email(message = "Invalid email format") String email,
        @NotBlank(message = "Password cannot be empty") String password,
        @NotNull Set<Role> roles
) {}