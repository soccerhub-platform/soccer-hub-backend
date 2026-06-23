package kz.edu.soccerhub.common.dto.payment;

import java.util.List;

public record PaymentsPageOutput(
        List<PaymentOutput> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}
