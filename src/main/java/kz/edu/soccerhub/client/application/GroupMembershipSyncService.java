package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.GroupMembership;
import kz.edu.soccerhub.common.port.GroupMembershipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMembershipSyncService {

    private final GroupMembershipPort groupMembershipPort;

    @Transactional
    public void syncFromContract(Contract contract) {
        if (groupMembershipPort.findBySourceContractId(contract.getId()).isPresent()) {
            return;
        }

        GroupMembership membership = GroupMembership.builder()
                .id(UUID.randomUUID())
                .groupId(contract.getGroupId())
                .playerId(contract.getPlayerId())
                .status(resolveStatus(contract))
                .joinedAt(contract.getStartDate())
                .leftAt(resolveLeftAt(contract))
                .sourceContractId(contract.getId())
                .build();

        groupMembershipPort.save(membership);
    }

    GroupMembershipStatus resolveStatus(Contract contract) {
        LocalDate today = LocalDate.now();
        if (contract.getStatus() == ContractStatus.CANCELLED) {
            return GroupMembershipStatus.REMOVED;
        }
        if (contract.getStartDate() != null && contract.getStartDate().isAfter(today)) {
            return GroupMembershipStatus.UPCOMING;
        }
        if (contract.getEndDate() != null && contract.getEndDate().isBefore(today)) {
            return GroupMembershipStatus.COMPLETED;
        }
        return GroupMembershipStatus.ACTIVE;
    }

    LocalDate resolveLeftAt(Contract contract) {
        if (contract.getStatus() != ContractStatus.CANCELLED) {
            return contract.getEndDate();
        }

        LocalDate today = LocalDate.now();
        if (contract.getEndDate() != null && contract.getEndDate().isBefore(today)) {
            return contract.getEndDate();
        }
        if (contract.getStartDate() != null && contract.getStartDate().isAfter(today)) {
            return contract.getStartDate();
        }
        return today;
    }
}
