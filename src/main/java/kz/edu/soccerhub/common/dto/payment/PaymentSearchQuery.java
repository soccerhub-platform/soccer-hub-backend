package kz.edu.soccerhub.common.dto.payment;

import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record PaymentSearchQuery(
        UUID branchId,
        UUID contractId,
        UUID clientId,
        Set<PaymentStatus> statuses,
        Set<PaymentMethod> methods,
        LocalDateTime paidFrom,
        LocalDateTime paidTo
) {
}
