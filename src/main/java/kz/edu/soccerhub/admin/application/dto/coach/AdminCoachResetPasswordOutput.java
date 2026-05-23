package kz.edu.soccerhub.admin.application.dto.coach;

import lombok.Builder;

@Builder
public record AdminCoachResetPasswordOutput(String temporaryPassword) {
}
