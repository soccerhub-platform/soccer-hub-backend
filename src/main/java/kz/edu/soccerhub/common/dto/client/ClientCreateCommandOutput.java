package kz.edu.soccerhub.common.dto.client;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ClientCreateCommandOutput(
        UUID clientId
) {
}
