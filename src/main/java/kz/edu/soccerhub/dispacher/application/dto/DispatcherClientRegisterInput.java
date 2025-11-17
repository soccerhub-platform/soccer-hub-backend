package kz.edu.soccerhub.dispacher.application.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherClientRegisterInput(
        String firstName,
        String lastName,
        String phoneNumber,
        UUID branchId,
        String source,
        String comments
) {
}
