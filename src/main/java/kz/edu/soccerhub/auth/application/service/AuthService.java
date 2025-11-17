package kz.edu.soccerhub.auth.application.service;

import jakarta.validation.Valid;
import kz.edu.soccerhub.auth.application.dto.*;
import kz.edu.soccerhub.auth.domain.model.AppRoleEntity;
import kz.edu.soccerhub.auth.domain.model.AppUserEntity;
import kz.edu.soccerhub.auth.domain.model.vo.RotatedRefreshToken;
import kz.edu.soccerhub.auth.domain.model.vo.Tokens;
import kz.edu.soccerhub.auth.domain.repository.AppUserRepo;
import kz.edu.soccerhub.auth.security.AppUserDetails;
import kz.edu.soccerhub.auth.security.JwtProperties;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.RegisterCommand;
import kz.edu.soccerhub.common.dto.auth.RegisterCommandOutput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.UnauthorizedException;
import kz.edu.soccerhub.common.port.AuthPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements AuthPort {

    private final TokenService tokenService;
    private final AuthenticationManager authManager;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepo userRepo;

    @Override
    public RegisterCommandOutput register(RegisterCommand command) {
        String normalizedEmail = normalizeEmail(command.email());

        userRepo.findByEmail(normalizedEmail).ifPresent(u -> {
            throw new BadRequestException("Email is already registered", u.getEmail());
        });

        AppUserEntity user = buildAppUser(normalizedEmail, command.password(), command.roles());
        userRepo.save(user);

        return new RegisterCommandOutput(user.getId(), user.getEmail());
    }
    public TokenOutput login(@Valid LoginInput input, String userAgent) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.email(), input.password())
        );

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        AppUserEntity user = principal.getUser();

        Set<Role> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        if (roles.isEmpty()) {
            throw new UnauthorizedException("User has no roles assigned");
        }
        if (!roles.contains(input.role())) {
            throw new UnauthorizedException("User does not have the required role: " + input.role());
        }

        Tokens tokens = tokenService.issueTokens(user, Set.of(input.role()), userAgent);

        return new TokenOutput(
                tokens.accessToken(),
                tokens.refreshToken(),
                jwtProperties.getAccessTtl().toSeconds()
        );
    }

    public TokenOutput refresh(RefreshInput request, String userAgent) {
        RotatedRefreshToken newRotatedRefreshTokenToken = tokenService.validateAndRotateRefresh(request.refreshToken(), userAgent)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired"));

        AppUserEntity user = newRotatedRefreshTokenToken.user();
        Set<Role> roles = user.getRoles().stream().map(AppRoleEntity::getCode).collect(Collectors.toSet());

        String newAccessToken = tokenService.issueAccessToken(user, roles);

        return new TokenOutput(
                newAccessToken,
                newRotatedRefreshTokenToken.newRefreshToken(),
                jwtProperties.getAccessTtl().toSeconds()
        );
    }

    public Optional<Set<Role>> getCurrentUserRoles() {
        return getCurrentUserDetails()
                .map(user -> user.getUser()
                        .getRoles().stream()
                        .filter(Objects::nonNull)
                        .map(AppRoleEntity::getCode)
                        .collect(Collectors.toSet()));
    }

    public Optional<String> getCurrentUserEmail() {
        return getCurrentUserDetails()
                .map(user -> user.getUser().getEmail());
    }

    public Optional<String> getCurrentUserId() {
        return getCurrentUserDetails()
                .map(user -> user.getUser().getEmail());
    }

    // -------------------- Helpers --------------------

    private AppUserEntity buildAppUser(String email, String rawPassword, Set<Role> roles) {
        Set<AppRoleEntity> appRoles = roles.stream()
                .map(role -> AppRoleEntity.builder().code(role).build())
                .collect(Collectors.toSet());

        return AppUserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .enabled(true)
                .roles(appRoles)
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private Optional<Authentication> getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && authentication.isAuthenticated())
                ? Optional.of(authentication)
                : Optional.empty();
    }

    private Optional<AppUserDetails> getCurrentUserDetails() {
        return getAuthentication()
                .filter(auth -> auth.getPrincipal() instanceof AppUserDetails)
                .map(auth -> (AppUserDetails) auth.getPrincipal());
    }

}