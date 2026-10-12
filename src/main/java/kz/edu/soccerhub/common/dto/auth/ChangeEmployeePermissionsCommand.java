package kz.edu.soccerhub.common.dto.auth;
import kz.edu.soccerhub.common.domain.enums.EmployeePermission;
import lombok.Builder;
import java.util.Set;
import java.util.UUID;
@Builder
public record ChangeEmployeePermissionsCommand(UUID userId, UUID changedBy, Set<EmployeePermission> permissions) {}
