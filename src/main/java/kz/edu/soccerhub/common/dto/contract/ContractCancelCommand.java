package kz.edu.soccerhub.common.dto.contract;

import kz.edu.soccerhub.client.domain.enums.ContractCancelReasonCode;

public record ContractCancelCommand(
        ContractCancelReasonCode reasonCode,
        String comment
) {
}
