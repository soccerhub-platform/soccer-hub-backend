package kz.edu.soccerhub.client.application.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ClientDto(
        UUID id,
        String name,
        String phone,
        String status
) {
}
