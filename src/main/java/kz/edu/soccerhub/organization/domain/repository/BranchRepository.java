package kz.edu.soccerhub.organization.domain.repository;

import kz.edu.soccerhub.organization.domain.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {
    List<Branch> findAllByClubIdIn(Collection<UUID> clubIds);

    List<Branch> findByClubId(UUID clubId);
}
