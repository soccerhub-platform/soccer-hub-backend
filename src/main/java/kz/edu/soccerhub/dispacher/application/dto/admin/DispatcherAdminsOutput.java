package kz.edu.soccerhub.dispacher.application.dto.admin;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherAdminsOutput(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean isActive,
        Club club,
        Branch branch

) {

    @Builder
    public record Club(
            UUID id,
            String name
    ) {}

    @Builder
    public record Branch(
            UUID id,
            String name
    ) { }
}
