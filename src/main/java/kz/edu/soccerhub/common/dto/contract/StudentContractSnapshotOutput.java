package kz.edu.soccerhub.common.dto.contract;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StudentContractSnapshotOutput(
        UUID id,
        UUID playerId,
        UUID branchId,
        String contractNumber,
        ContractStatus status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal amount,
        String currency,
        UUID groupId,
        String groupName,
        UUID coachId,
        String coachName
) {
}
