package kz.edu.soccerhub.common.dto.contract;

public record ContractPrimaryContactDraftInput(
        String fullName,
        String phone,
        String email,
        String source,
        String comments
) {
}
