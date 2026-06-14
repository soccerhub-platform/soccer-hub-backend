package kz.edu.soccerhub.common.dto.contract;

public record ContractValidationError(
        String code,
        String field,
        String message
) {
}
