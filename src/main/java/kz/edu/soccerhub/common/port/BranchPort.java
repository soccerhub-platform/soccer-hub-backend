package kz.edu.soccerhub.common.port;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.branch.CreateBranchCommand;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface BranchPort {
    UUID create(CreateBranchCommand command);
    boolean isExist(@NotNull  UUID branchId);
    Optional<BranchDto> findById(@NotNull UUID branchId);
    Collection<BranchDto> findAllByIds(Collection<UUID> ids);
    Collection<BranchDto> findByClubId(@NotNull UUID clubId);
    Collection<BranchDto> findByClubIds(Collection<UUID> ids);
    void delete(@NotNull UUID branchId);
}