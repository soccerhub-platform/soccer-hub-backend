package kz.edu.soccerhub.dispacher.application.dto;

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