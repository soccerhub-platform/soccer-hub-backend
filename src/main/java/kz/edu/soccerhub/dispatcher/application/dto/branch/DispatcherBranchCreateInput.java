package kz.edu.soccerhub.dispatcher.application.dto.branch;

import jakarta.validation.constraints.*;
import kz.edu.soccerhub.common.dto.branch.BranchWorkingDay;
import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Builder
public record DispatcherBranchCreateInput(
        @NotNull UUID clubId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String address,
        @Pattern(regexp = "^$|^\\+[1-9][0-9]{6,14}$") String phone,
        @Email @Size(max = 254) String email,
        UUID managerAdminId,
        @Size(max = 7) List<BranchWorkingDay> workingHours
) {}
