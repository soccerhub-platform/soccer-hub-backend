package kz.edu.soccerhub.admin.application.dto.group;

public record AdminGroupMembershipTransferOutput(
        AdminGroupMembershipOutput previousMembership,
        AdminGroupMembershipOutput newMembership
) {
}
