// UserService.java
package kz.edu.soccerhub.service;

import kz.edu.soccerhub.dto.RegisterRequest;
import kz.edu.soccerhub.dto.RegisterResponse;
import kz.edu.soccerhub.model.AppRole;
import kz.edu.soccerhub.model.AppUser;
import kz.edu.soccerhub.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public RegisterResponse register(RegisterRequest request) {
        // check if email exists
        userRepo.findByEmail(request.email())
                .ifPresent(u -> { throw new RuntimeException("Email already registered"); });

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRoles(Set.of(AppRole.builder().code("LEAD").build()));

        userRepo.save(user);
        return new RegisterResponse(user.getId(), user.getEmail());
    }
}