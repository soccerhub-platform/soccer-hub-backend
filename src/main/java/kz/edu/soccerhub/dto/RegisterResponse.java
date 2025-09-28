// RegisterResponse.java
package kz.edu.soccerhub.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email
) {}