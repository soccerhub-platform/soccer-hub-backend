package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.domain.enums.Role;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AuthPort {
    UUID register(String email, String password, Set<Role> roles);
    String login(String email, String password, String userAgent);
    String refresh(String refreshToken, String userAgent);
    Optional<Set<Role>> getCurrentUserRoles();
    Optional<String> getCurrentUserEmail();
    Optional<String> getCurrentUserId();
}
