package kz.edu.soccerhub.common.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.domain.enums.Role;
import lombok.Builder;

import java.util.Set;

@Builder
public record AuthRegisterCommand(
        @Email(message = "Invalid email format") String email,
        @NotBlank(message = "Password cannot be empty") String password,
        @NotNull Set<Role> roles
) {
}
