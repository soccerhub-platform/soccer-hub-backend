// RegisterResponse.java
package kz.edu.soccerhub.auth.application.dto;

import java.util.UUID;

public record RegisterOutput(
        UUID id,
        String email
) {}