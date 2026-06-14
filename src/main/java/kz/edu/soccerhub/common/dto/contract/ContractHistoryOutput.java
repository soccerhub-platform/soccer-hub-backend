package kz.edu.soccerhub.common.dto.contract;

import kz.edu.soccerhub.client.domain.enums.ContractHistoryType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContractHistoryOutput(
        UUID id,
        ContractHistoryType type,
        LocalDateTime createdAt,
        String actorName,
        String comment
) {
}
