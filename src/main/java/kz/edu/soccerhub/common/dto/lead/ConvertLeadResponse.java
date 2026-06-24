package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ConvertLeadResponse(
        UUID leadId,
        LeadStatus leadStatus,
        UUID clientId,
        String clientName,
        UUID playerId,
        String playerName,
        UUID contractId,
        String contractNumber,
        ContractStatus contractStatus,
        ContractPaymentStatus paymentStatus,
        BigDecimal amount,
        BigDecimal outstandingAmount,
        String status
) {
}
