package kz.edu.soccerhub.admin.application.dto.trial;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AdminCancelTrialInput(@NotBlank String reason) {
}
