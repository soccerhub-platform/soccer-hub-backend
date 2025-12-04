package kz.edu.soccerhub.dispacher.application.dto.club;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherClubsOutput(
        UUID clubId,
        String name,
        String slug,
        String phoneNumber,
        String address
) { }