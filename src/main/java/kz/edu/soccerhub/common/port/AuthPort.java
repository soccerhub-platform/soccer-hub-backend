package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.RegisterCommand;
import kz.edu.soccerhub.common.dto.auth.RegisterCommandOutput;

import java.util.Optional;
import java.util.Set;

public interface AuthPort {
    RegisterCommandOutput register(RegisterCommand registerCommand);
    Optional<Set<Role>> getCurrentUserRoles();
    Optional<String> getCurrentUserEmail();
    Optional<String> getCurrentUserId();
}
