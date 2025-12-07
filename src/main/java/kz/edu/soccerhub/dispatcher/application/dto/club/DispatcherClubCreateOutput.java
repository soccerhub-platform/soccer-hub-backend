package kz.edu.soccerhub.dispatcher.application.dto.club;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherClubCreateOutput(
        UUID clubId
) {}