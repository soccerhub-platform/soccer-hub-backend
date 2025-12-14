package kz.edu.soccerhub.admin.domain.repository;

import kz.edu.soccerhub.admin.domain.model.AdminBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface AdminBranchRepository extends JpaRepository<AdminBranch, UUID> {

    Collection<AdminBranch> findAllByBranchId(UUID branchId);

    boolean existsByAdminIdAndBranchId(UUID adminId, UUID branchId);

    Collection<AdminBranch> findAllByAdminId(UUID adminId);

}
