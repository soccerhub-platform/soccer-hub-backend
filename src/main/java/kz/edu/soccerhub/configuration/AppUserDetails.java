package kz.edu.soccerhub.configuration;

import kz.edu.soccerhub.model.AppRole;
import kz.edu.soccerhub.model.AppUser;
import lombok.AllArgsConstructor;
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

    private final AppUser user;

    public AppUserDetails(AppUser user) {
        this.user = Objects.requireNonNull(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<AppRole> roles = user.getRoles();
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode()))
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
