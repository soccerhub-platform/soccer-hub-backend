package kz.edu.soccerhub.admin.application.dto.student;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;

import java.util.UUID;

public record AdminStudentsQuery(
        UUID branchId,
        String search,
        ContractPaymentStatus paymentStatus,
        ContractStatus contractStatus,
        AdminStudentRiskCode risk,
        UUID groupId
) {
}
