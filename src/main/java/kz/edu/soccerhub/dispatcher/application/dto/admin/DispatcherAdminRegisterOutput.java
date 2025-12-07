package kz.edu.soccerhub.dispatcher.application.dto.admin;

import java.util.UUID;

public record DispatcherAdminRegisterOutput(
        UUID userId,
        String tempPassword
) {
}
