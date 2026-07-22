package kz.edu.soccerhub.common.dto.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClientWorkspaceDetailsOutput(
        ClientBlock client,
        SummaryBlock summary,
        List<ClientStudentRelationOutput> students,
        CapabilitiesBlock capabilities
) {
    public record ClientBlock(
            UUID id,
            UUID branchId,
            String fullName,
            String firstName,
            String lastName,
            String phone,
            String email,
            String status,
            String source,
            String comments,
            LocalDateTime createdAt
    ) {
    }

    public record SummaryBlock(int studentsCount, int activeContractsCount, int allContractsCount, MoneyBlock money) {
    }

    public record MoneyBlock(
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            BigDecimal overpaidAmount,
            String currency,
            boolean mixedCurrencies,
            LocalDateTime lastPaidAt
    ) {
    }

    public record CapabilitiesBlock(
            boolean canEdit,
            boolean canLinkStudent,
            boolean canCreateContract,
            boolean canRecordPayment,
            boolean canActivate,
            boolean canPause,
            boolean canDeactivate
    ) {
    }
}
