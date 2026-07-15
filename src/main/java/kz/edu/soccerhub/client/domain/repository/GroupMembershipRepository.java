package kz.edu.soccerhub.client.domain.repository;

import kz.edu.soccerhub.client.domain.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {

    Optional<GroupMembership> findBySourceContractId(UUID sourceContractId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select gm
            from GroupMembership gm
            where gm.id = :membershipId
            """)
    Optional<GroupMembership> findByIdForUpdate(UUID membershipId);

    @Query("""
            select gm
            from GroupMembership gm
            where gm.groupId = :groupId
              and gm.status in (
                  kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus.ACTIVE,
                  kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus.UPCOMING
              )
              and gm.joinedAt <= :asOfDate
              and (gm.leftAt is null or gm.leftAt >= :asOfDate)
            """)
    List<GroupMembership> findActiveByGroupIdAsOfDate(UUID groupId, LocalDate asOfDate);

    @Query("""
            select count(gm)
            from GroupMembership gm
            where gm.groupId = :groupId
              and gm.status in (
                  kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus.ACTIVE,
                  kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus.UPCOMING
              )
              and gm.joinedAt <= :asOfDate
              and (gm.leftAt is null or gm.leftAt >= :asOfDate)
            """)
    long countActiveByGroupIdAsOfDate(UUID groupId, LocalDate asOfDate);

    @Query("""
            select (count(gm) > 0)
            from GroupMembership gm
            where gm.groupId = :groupId
              and gm.playerId = :playerId
              and gm.status in (
                  kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus.ACTIVE,
                  kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus.UPCOMING
              )
              and gm.joinedAt <= :asOfDate
              and (gm.leftAt is null or gm.leftAt >= :asOfDate)
            """)
    boolean existsActiveByGroupIdAndPlayerIdAsOfDate(UUID groupId, UUID playerId, LocalDate asOfDate);

    @Query("""
            select gm
            from GroupMembership gm
            where gm.playerId in :playerIds
              and gm.status in (
                  kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus.ACTIVE,
                  kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus.UPCOMING
              )
              and gm.joinedAt <= :asOfDate
              and (gm.leftAt is null or gm.leftAt >= :asOfDate)
            """)
    List<GroupMembership> findActiveByPlayerIdInAsOfDate(Collection<UUID> playerIds, LocalDate asOfDate);

    @Query("""
            select gm
            from GroupMembership gm
            where gm.groupId = :groupId
              and gm.playerId = :playerId
              and gm.joinedAt <= :asOfDate
              and (gm.leftAt is null or gm.leftAt >= :asOfDate)
            order by
              case when gm.leftAt is null then 1 else 0 end desc,
              gm.leftAt desc,
              gm.joinedAt desc
            """)
    List<GroupMembership> findByGroupIdAndPlayerIdAsOfDateOrdered(UUID groupId, UUID playerId, LocalDate asOfDate);

    @Query("""
            select gm
            from GroupMembership gm
            where gm.groupId = :groupId
              and gm.playerId in :playerIds
              and (gm.leftAt is null or gm.leftAt >= :asOfDate)
            """)
    List<GroupMembership> findByGroupIdAndPlayerIdInEndingOnOrAfterDate(UUID groupId, Collection<UUID> playerIds, LocalDate asOfDate);

    List<GroupMembership> findByPlayerIdOrderByJoinedAtDesc(UUID playerId);
}
