package kz.edu.soccerhub.branch.domain.repository;

import kz.edu.soccerhub.branch.domain.model.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {
    List<BranchEntity> findAllByClubIdIn(Collection<UUID> clubIds);

    List<BranchEntity> findByClubId(UUID clubId);
}
