package kz.edu.soccerhub.common.dto.payment;

import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentOutput(
        UUID id,
        UUID contractId,
        String contractNumber,
        UUID clientId,
        String clientName,
        UUID playerId,
        String playerName,
        UUID branchId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime paidAt,
        LocalDateTime recordedAt,
        UUID recordedBy,
        String recordedByName,
        String comment,
        String externalReference,
        String cancelReason,
        String cancelComment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
