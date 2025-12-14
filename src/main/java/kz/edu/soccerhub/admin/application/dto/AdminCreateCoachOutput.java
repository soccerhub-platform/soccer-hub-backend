package kz.edu.soccerhub.admin.application.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminCreateCoachOutput(
        UUID coachId
) {
}
