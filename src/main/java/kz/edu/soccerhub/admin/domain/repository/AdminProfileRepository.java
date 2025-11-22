package kz.edu.soccerhub.admin.domain.repository;

import kz.edu.soccerhub.admin.domain.model.AdminProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface AdminProfileRepository extends JpaRepository<AdminProfileEntity, UUID> {
    Collection<AdminProfileEntity> findAllByBranchId(UUID branchId);
}
