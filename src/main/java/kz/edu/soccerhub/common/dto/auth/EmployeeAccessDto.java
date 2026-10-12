package kz.edu.soccerhub.common.dto.auth;
import kz.edu.soccerhub.common.domain.enums.*;
import lombok.Builder;
import java.util.Set;
@Builder
public record EmployeeAccessDto(boolean enabled, Set<Role> roles, Set<EmployeePermission> permissions) {}
