package kz.edu.soccerhub.auth.application.adapter;

import kz.edu.soccerhub.auth.application.dto.LoginInput;
import kz.edu.soccerhub.auth.application.dto.RefreshInput;
import kz.edu.soccerhub.auth.application.dto.RegisterInput;
import kz.edu.soccerhub.auth.application.service.AuthService;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.port.AuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthAdapter implements AuthPort {

    private final AuthService authService;

    @Override
    public String login(String email, String password, String userAgent) {
        return authService.login(new LoginInput(email, password), userAgent)
                .accessToken();
    }


    @Override
    public UUID register(String email, String password, Set<Role> roles) {
        return authService.register(new RegisterInput(email, password, roles))
                .id();
    }

    @Override
    public String refresh(String refreshToken, String userAgent) {
        return authService.refresh(new RefreshInput(refreshToken), userAgent)
                .accessToken();
    }

    @Override
    public Optional<Set<Role>> getCurrentUserRoles() {
        return authService.getCurrentUserRoles();
    }

    @Override
    public Optional<String> getCurrentUserEmail() {
        return authService.getCurrentUserEmail();
    }

    @Override
    public Optional<String> getCurrentUserId() {
        return authService.getCurrentUserId();
    }
}