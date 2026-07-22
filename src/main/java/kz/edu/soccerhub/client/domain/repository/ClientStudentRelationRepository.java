package kz.edu.soccerhub.client.domain.repository;

import kz.edu.soccerhub.client.domain.model.ClientStudentRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientStudentRelationRepository extends JpaRepository<ClientStudentRelation, UUID> {

    boolean existsByClientIdAndPlayerIdAndEndedAtIsNull(UUID clientId, UUID playerId);

    List<ClientStudentRelation> findByClientIdOrderByStartedAtDesc(UUID clientId);

    List<ClientStudentRelation> findByClientIdInAndEndedAtIsNull(Collection<UUID> clientIds);

    List<ClientStudentRelation> findByPlayerIdOrderByStartedAtDesc(UUID playerId);

    List<ClientStudentRelation> findByPlayerIdAndEndedAtIsNull(UUID playerId);

    Optional<ClientStudentRelation> findFirstByPlayerIdAndPrimaryContactTrueAndEndedAtIsNullOrderByStartedAtDesc(UUID playerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ClientStudentRelation> findLockedById(UUID id);
}
