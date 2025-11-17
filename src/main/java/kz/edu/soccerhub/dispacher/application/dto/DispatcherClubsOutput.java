package kz.edu.soccerhub.dispacher.application.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record DispatcherClubsOutput(
        List<DispatcherClubDto> clubs
) {

    @Builder
    public record DispatcherClubDto(
            UUID clubId,
            String name,
            String slug
    ) {}

}