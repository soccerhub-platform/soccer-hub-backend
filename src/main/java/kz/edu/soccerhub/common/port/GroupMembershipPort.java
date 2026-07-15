package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.organization.domain.model.GroupMembership;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMembershipPort {

    Optional<GroupMembership> findBySourceContractId(UUID sourceContractId);

    Optional<GroupMembership> findByIdForUpdate(UUID membershipId);

    List<GroupMembership> findActiveByGroupIdAsOfDate(UUID groupId, LocalDate asOfDate);

    long countActiveByGroupIdAsOfDate(UUID groupId, LocalDate asOfDate);

    boolean existsActiveByGroupIdAndPlayerIdAsOfDate(UUID groupId, UUID playerId, LocalDate asOfDate);

    List<GroupMembership> findActiveByPlayerIdInAsOfDate(Collection<UUID> playerIds, LocalDate asOfDate);

    Optional<GroupMembership> findByGroupIdAndPlayerIdAsOfDate(UUID groupId, UUID playerId, LocalDate asOfDate);

    List<GroupMembership> findByGroupIdAndPlayerIdInEndingOnOrAfterDate(UUID groupId, Collection<UUID> playerIds, LocalDate asOfDate);

    List<GroupMembership> findByPlayerIdOrderByJoinedAtDesc(UUID playerId);

    GroupMembership save(GroupMembership membership);
}
