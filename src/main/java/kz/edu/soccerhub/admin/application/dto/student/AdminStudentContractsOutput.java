package kz.edu.soccerhub.admin.application.dto.student;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminStudentContractsOutput(
        UUID playerId,
        Summary summary,
        List<Item> items
) {
    public record Summary(
            int totalCount,
            int activeCount,
            int upcomingCount,
            int endingSoonCount,
            int withDebtCount
    ) {
    }

    public record Item(
            UUID id,
            String contractNumber,
            ContractStatus status,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amount,
            String currency,
            GroupRef group,
            String coachName,
            ContractPaymentStatus paymentStatus,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            BigDecimal overpaidAmount,
            LocalDateTime lastPaidAt,
            int paymentsCount,
            boolean current
    ) {
    }

    public record GroupRef(
            UUID id,
            String name,
            MediaAssetResponse avatar
    ) {
    }
}
