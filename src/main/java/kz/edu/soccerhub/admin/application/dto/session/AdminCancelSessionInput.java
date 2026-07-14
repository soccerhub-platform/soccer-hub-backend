package kz.edu.soccerhub.admin.application.dto.session;

import jakarta.validation.constraints.NotBlank;

public record AdminCancelSessionInput(
        @NotBlank
        String reasonCode,
        String comment
) {
}
