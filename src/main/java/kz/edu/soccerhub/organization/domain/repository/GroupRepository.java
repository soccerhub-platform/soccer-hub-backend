package kz.edu.soccerhub.organization.domain.repository;

import kz.edu.soccerhub.organization.domain.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    Collection<Group> findByBranchId(UUID branchId);
}
