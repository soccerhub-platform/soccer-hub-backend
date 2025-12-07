package kz.edu.soccerhub.dispatcher.application.dto;

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
