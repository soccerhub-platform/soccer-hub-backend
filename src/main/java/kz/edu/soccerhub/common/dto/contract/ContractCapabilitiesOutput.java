package kz.edu.soccerhub.common.dto.contract;

public record ContractCapabilitiesOutput(
        boolean canEdit,
        boolean canActivate,
        boolean canExtend,
        boolean canCancel,
        boolean canAddPayment,
        boolean canEnrollStudent
) {
}
