package kz.edu.soccerhub.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kz.edu.soccerhub.configuration.AppUserDetails;
import kz.edu.soccerhub.configuration.JwtProperties;
import kz.edu.soccerhub.dto.ErrorResponse;
import kz.edu.soccerhub.dto.RegisterRequest;
import kz.edu.soccerhub.dto.RegisterResponse;
import kz.edu.soccerhub.model.AppRole;
import kz.edu.soccerhub.service.TokenService;
import kz.edu.soccerhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authManager;
    private final TokenService tokenService;
    private final UserService userService;
    private final JwtProperties jwtProperties;

    public record LoginRequest(String email, String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {}

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req,
                                               @RequestHeader(value="User-Agent", required=false) String ua) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        var principal = (AppUserDetails) auth.getPrincipal();
        var user = principal.getUser();
        var roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        var tokens = tokenService.issueTokens(user, roles, ua);
        return ResponseEntity.ok(new TokenResponse(
                tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn().toSeconds()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody @Valid RefreshRequest req,
                                     @RequestHeader(value = "User-Agent", required = false) String ua) {
        var resultOpt = tokenService.validateAndRotateRefresh(req.refreshToken(), ua);
        if (resultOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_REFRESH", "Refresh token is invalid or expired"));
        }

        var result = resultOpt.get();
        var user = result.user();
        var roles = user.getRoles().stream().map(AppRole::getCode).toList();

        String access = tokenService.issueAccessToken(user, roles);

        return ResponseEntity.ok(new TokenResponse(
                access, result.newRefresh(), jwtProperties.getAccessTtl().toSeconds()
        ));
    }
}
