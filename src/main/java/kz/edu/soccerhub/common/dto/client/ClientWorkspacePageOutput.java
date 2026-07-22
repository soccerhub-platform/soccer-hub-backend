package kz.edu.soccerhub.common.dto.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClientWorkspacePageOutput(
        List<Item> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        Summary summary
) {
    public record Summary(
            long clientsCount,
            long activeClientsCount,
            long studentsCount,
            long activeContractsCount,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            String currency,
            boolean mixedCurrencies
    ) {
    }

    public record Item(
            UUID id,
            String fullName,
            String phone,
            String email,
            String status,
            int studentsCount,
            int activeContractsCount,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            String currency,
            boolean mixedCurrencies,
            LocalDateTime lastPaidAt,
            String paymentStatus
    ) {
    }
}
