package kz.edu.soccerhub.organization.domain.repository;

import kz.edu.soccerhub.organization.domain.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Branch b where b.id = :id")
    Optional<Branch> findForUpdate(UUID id);
    List<Branch> findAllByClubIdIn(Collection<UUID> clubIds);

    List<Branch> findByClubId(UUID clubId);
}
