package kz.edu.soccerhub.common.dto.contract;

import java.util.List;

public record ContractValidationResult(
        boolean valid,
        List<ContractValidationError> errors
) {
}
