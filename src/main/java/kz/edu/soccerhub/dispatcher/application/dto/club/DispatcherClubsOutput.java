package kz.edu.soccerhub.dispatcher.application.dto.club;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherClubsOutput(
        UUID clubId,
        String name,
        String slug,
        String email,
        String phoneNumber,
        String address
) { }
