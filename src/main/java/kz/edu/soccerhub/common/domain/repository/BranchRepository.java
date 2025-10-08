package kz.edu.soccerhub.common.domain.repository;

import kz.edu.soccerhub.common.domain.model.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {
}
