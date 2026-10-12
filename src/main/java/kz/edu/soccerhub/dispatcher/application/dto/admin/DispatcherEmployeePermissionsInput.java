package kz.edu.soccerhub.dispatcher.application.dto.admin;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.domain.enums.EmployeePermission;
import lombok.Builder;
import java.util.Set;
@Builder
public record DispatcherEmployeePermissionsInput(@NotNull Set<EmployeePermission> permissions) {}
