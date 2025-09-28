// RegisterRequest.java
package kz.edu.soccerhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @Email(message = "Invalid email format") String email,
        @NotBlank(message = "Password cannot be empty") String password
) {}