package kz.edu.soccerhub.common.dto.student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StudentProfileDto(
        UUID branchId,
        UUID playerId,
        String playerFullName,
        LocalDateTime createdAt,
        LocalDate birthDate,
        UUID clientId,
        String clientFullName,
        String phone,
        String email,
        String clientStatus
) {
}
