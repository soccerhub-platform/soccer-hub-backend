package kz.edu.soccerhub.dispatcher.application.dto.admin;

import lombok.Builder;

@Builder
public record DispatcherAdminResetPasswordOutput(String temporaryPassword) {}