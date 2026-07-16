package kz.edu.soccerhub.organization.domain.repository;

import kz.edu.soccerhub.organization.domain.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                  kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.ACTIVE,
                  kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.UPCOMING
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
                  kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.ACTIVE,
                  kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.UPCOMING
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
                  kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.ACTIVE,
                  kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.UPCOMING
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
                  kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.ACTIVE,
                  kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.UPCOMING
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GroupMembership gm
            set gm.status = kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.COMPLETED,
                gm.updatedAt = :updatedAt,
                gm.modifiedBy = :modifiedBy,
                gm.version = gm.version + 1
            where gm.status in (
                kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.ACTIVE,
                kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.UPCOMING
            )
              and gm.leftAt is not null
              and gm.leftAt < :asOfDate
            """)
    int completeExpiredMemberships(
            @Param("asOfDate") LocalDate asOfDate,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("modifiedBy") String modifiedBy
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GroupMembership gm
            set gm.status = kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.ACTIVE,
                gm.updatedAt = :updatedAt,
                gm.modifiedBy = :modifiedBy,
                gm.version = gm.version + 1
            where gm.status = kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus.UPCOMING
              and gm.joinedAt <= :asOfDate
              and (gm.leftAt is null or gm.leftAt >= :asOfDate)
            """)
    int activateStartedMemberships(
            @Param("asOfDate") LocalDate asOfDate,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("modifiedBy") String modifiedBy
    );
}
