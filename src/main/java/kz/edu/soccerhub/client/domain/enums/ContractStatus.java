package kz.edu.soccerhub.client.domain.enums;

public enum ContractStatus {
    DRAFT,
    UPCOMING,
    ACTIVE,
    EXPIRED,
    CANCELLED;

    public boolean isEditable() {
        return this != CANCELLED && this != EXPIRED;
    }

    public boolean canBeExtended() {
        return this == UPCOMING || this == ACTIVE || this == EXPIRED;
    }
}
