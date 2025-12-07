package kz.edu.soccerhub.dispatcher.application.dto.club;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record DispatcherClubCreateInput(
        @NotBlank String name,
        @NotBlank String slug,
        String email,
        String phone,
        String address
) {}