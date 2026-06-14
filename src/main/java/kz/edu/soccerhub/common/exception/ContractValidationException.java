package kz.edu.soccerhub.common.exception;

import kz.edu.soccerhub.common.dto.contract.ContractValidationError;
import lombok.Getter;

import java.util.List;

@Getter
public class ContractValidationException extends RuntimeException {

    private final List<ContractValidationError> errors;

    public ContractValidationException(List<ContractValidationError> errors) {
        super("Contract validation failed");
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
