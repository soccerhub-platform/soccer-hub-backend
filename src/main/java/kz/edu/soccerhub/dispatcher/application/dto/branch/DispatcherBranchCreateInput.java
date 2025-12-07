package kz.edu.soccerhub.dispatcher.application.dto.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherBranchCreateInput(
        @NotNull UUID clubId,
        @NotBlank String name,
        String address
) {}