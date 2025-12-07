package kz.edu.soccerhub.dispatcher.application.dto.admin;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherAdminRegisterInput(
        @Email String email,
        String firstName,
        String lastName,
        String phone,
        @NotNull UUID assignedBranch
) { }
