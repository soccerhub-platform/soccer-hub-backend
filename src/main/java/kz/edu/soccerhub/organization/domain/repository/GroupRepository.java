package kz.edu.soccerhub.organization.domain.repository;

import kz.edu.soccerhub.organization.domain.model.Group;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    Collection<Group> findByBranchId(UUID branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select g
            from Group g
            where g.id = :groupId
            """)
    Optional<Group> findByIdForUpdate(UUID groupId);
}
