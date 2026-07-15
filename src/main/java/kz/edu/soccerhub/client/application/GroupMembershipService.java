package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.model.GroupMembership;
import kz.edu.soccerhub.client.domain.repository.GroupMembershipRepository;
import kz.edu.soccerhub.common.port.GroupMembershipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMembershipService implements GroupMembershipPort {

    private final GroupMembershipRepository groupMembershipRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<GroupMembership> findBySourceContractId(UUID sourceContractId) {
        return groupMembershipRepository.findBySourceContractId(sourceContractId);
    }

    @Override
    @Transactional
    public Optional<GroupMembership> findByIdForUpdate(UUID membershipId) {
        return groupMembershipRepository.findByIdForUpdate(membershipId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMembership> findActiveByGroupIdAsOfDate(UUID groupId, LocalDate asOfDate) {
        return groupMembershipRepository.findActiveByGroupIdAsOfDate(groupId, asOfDate);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByGroupIdAsOfDate(UUID groupId, LocalDate asOfDate) {
        return groupMembershipRepository.countActiveByGroupIdAsOfDate(groupId, asOfDate);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveByGroupIdAndPlayerIdAsOfDate(UUID groupId, UUID playerId, LocalDate asOfDate) {
        return groupMembershipRepository.existsActiveByGroupIdAndPlayerIdAsOfDate(groupId, playerId, asOfDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMembership> findActiveByPlayerIdInAsOfDate(Collection<UUID> playerIds, LocalDate asOfDate) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }
        return groupMembershipRepository.findActiveByPlayerIdInAsOfDate(playerIds, asOfDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GroupMembership> findByGroupIdAndPlayerIdAsOfDate(UUID groupId, UUID playerId, LocalDate asOfDate) {
        return groupMembershipRepository.findByGroupIdAndPlayerIdAsOfDateOrdered(groupId, playerId, asOfDate).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMembership> findByGroupIdAndPlayerIdInEndingOnOrAfterDate(UUID groupId, Collection<UUID> playerIds, LocalDate asOfDate) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }
        return groupMembershipRepository.findByGroupIdAndPlayerIdInEndingOnOrAfterDate(groupId, playerIds, asOfDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMembership> findByPlayerIdOrderByJoinedAtDesc(UUID playerId) {
        return groupMembershipRepository.findByPlayerIdOrderByJoinedAtDesc(playerId);
    }

    @Override
    @Transactional
    public GroupMembership save(GroupMembership membership) {
        return groupMembershipRepository.save(membership);
    }
}
