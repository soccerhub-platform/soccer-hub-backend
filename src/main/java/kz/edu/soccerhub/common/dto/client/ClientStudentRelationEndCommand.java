package kz.edu.soccerhub.common.dto.client;

import java.time.LocalDate;
import java.util.UUID;

public record ClientStudentRelationEndCommand(
        UUID relationId,
        LocalDate endedAt
) {
}
