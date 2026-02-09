package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AuthPort {
    AuthRegisterCommandOutput register(AuthRegisterCommand authRegisterCommand);
    Optional<Set<Role>> getCurrentUserRoles();
    Optional<String> getCurrentUserEmail();
    Optional<String> getCurrentUserId();
    void delete(UUID userId);
    void resetPassword(UUID userId, String rawPassword);
    void changePassword(UUID userId, String newPassword);
    void disableUser(UUID userId);
    void enableUser(UUID userId);
}
