package kz.edu.soccerhub.common.dto.contract;

import java.util.List;

public record ContractsPageOutput(
        List<ContractListItemOutput> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}
