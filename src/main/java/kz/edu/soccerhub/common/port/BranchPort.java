package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.branch.CreateBranchCommand;

import java.util.Collection;
import java.util.UUID;

public interface BranchPort {
    UUID create(CreateBranchCommand command);
    boolean isExist(UUID branchId);
    Collection<BranchDto> findAlByIds(Collection<UUID> ids);
    Collection<BranchDto> findByClubId(UUID clubId);
    Collection<BranchDto> findByClubIds(Collection<UUID> ids);
}