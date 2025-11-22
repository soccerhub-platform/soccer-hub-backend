package kz.edu.soccerhub.auth.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.auth.application.dto.LoginInput;
import kz.edu.soccerhub.auth.application.dto.RefreshInput;
import kz.edu.soccerhub.auth.application.dto.TokenOutput;
import kz.edu.soccerhub.auth.application.service.AuthService;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenOutput> login(@RequestBody @Valid LoginInput input,
                                             @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        return ResponseEntity.ok(authService.login(input, userAgent));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenOutput> refresh(@RequestBody @Valid RefreshInput input,
                                               @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        return ResponseEntity.ok(authService.refresh(input, userAgent));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthRegisterCommandOutput> register(@RequestBody @Valid AuthRegisterCommand command) {
        return ResponseEntity.ok(authService.register(command));
    }
}