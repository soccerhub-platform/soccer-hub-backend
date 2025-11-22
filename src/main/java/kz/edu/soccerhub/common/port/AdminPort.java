package kz.edu.soccerhub.common.port;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommand;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommandOutput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.admin.AdminUpdateCommand;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AdminPort {
    AdminCreateCommandOutput create(AdminCreateCommand command);
    Optional<AdminDto> findById(UUID adminId);
    Collection<AdminDto> findAllByBranchId(UUID branchId);
    void changeStatus(@NotNull UUID adminId, boolean active);
    void assignToBranch(@NotNull UUID adminId, @NotNull(message = "Branch ID cannot be null") UUID uuid);
    void updateAdminInfo(@NotNull UUID adminId, @NotNull AdminUpdateCommand build);
}
