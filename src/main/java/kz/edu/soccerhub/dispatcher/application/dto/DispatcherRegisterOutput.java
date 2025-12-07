package kz.edu.soccerhub.dispatcher.application.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherRegisterOutput(
        UUID userId,
        String tempPassword
) {
}
