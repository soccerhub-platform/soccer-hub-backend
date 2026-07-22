package kz.edu.soccerhub.admin.application.dto.client;

import jakarta.validation.constraints.NotBlank;

public record AdminClientStatusInput(@NotBlank String status) {
}
