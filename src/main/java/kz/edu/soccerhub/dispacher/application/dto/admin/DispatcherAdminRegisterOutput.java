package kz.edu.soccerhub.dispacher.application.dto.admin;

import java.util.UUID;

public record DispatcherAdminRegisterOutput(
        UUID userId,
        String tempPassword
) {
}
