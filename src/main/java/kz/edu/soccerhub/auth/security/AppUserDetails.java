package kz.edu.soccerhub.auth.security;

import kz.edu.soccerhub.auth.domain.model.AppRoleEntity;
import kz.edu.soccerhub.auth.domain.model.AppUserEntity;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class AppUserDetails implements UserDetails {

    private final AppUserEntity user;

    public AppUserDetails(AppUserEntity user) {
        this.user = Objects.requireNonNull(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<AppRoleEntity> roles = user.getRoles();
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode().name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // or use a field from AppUser
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // or use a field from AppUser
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // or use a field from AppUser
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
