package kz.edu.soccerhub.common.dto.student;

import java.time.LocalDate;
import java.util.UUID;

public record StudentProfileDto(
        UUID branchId,
        UUID playerId,
        String playerFullName,
        LocalDate birthDate,
        UUID clientId,
        String clientFullName,
        String phone,
        String email,
        String clientStatus
) {
}
