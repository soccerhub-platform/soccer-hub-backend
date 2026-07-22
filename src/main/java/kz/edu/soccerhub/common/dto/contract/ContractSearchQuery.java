package kz.edu.soccerhub.common.dto.contract;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;

import java.util.Set;
import java.util.UUID;

public record ContractSearchQuery(
        UUID branchId,
        UUID clientId,
        Set<ContractStatus> statuses,
        LeadType leadType,
        String search
) {
}
