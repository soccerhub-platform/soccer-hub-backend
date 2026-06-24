package kz.edu.soccerhub.admin.application.dto.student;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminStudentListItemOutput(
        UUID playerId,
        String playerName,
        LocalDate birthDate,
        Integer age,
        UUID clientId,
        String parentName,
        String phone,
        String email,
        UUID groupId,
        String groupName,
        String coachName,
        UUID contractId,
        String contractNumber,
        ContractStatus contractStatus,
        LocalDate contractEndDate,
        ContractPaymentStatus paymentStatus,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        Integer attendanceRate,
        Integer missedLast30Days,
        List<AdminStudentRiskOutput> risks
) {
}
