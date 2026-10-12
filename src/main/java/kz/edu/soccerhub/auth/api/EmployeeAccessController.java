package kz.edu.soccerhub.auth.api;
import kz.edu.soccerhub.common.dto.auth.EmployeeAccessDto;
import kz.edu.soccerhub.common.port.EmployeeAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/auth/access")
@RequiredArgsConstructor
public class EmployeeAccessController {
 private final EmployeeAccessPort access;
 @GetMapping
 public EmployeeAccessDto current(@AuthenticationPrincipal Jwt jwt) {
  if(jwt==null) throw new kz.edu.soccerhub.common.exception.UnauthorizedException("Войдите в систему");
  return access.getAccess(UUID.fromString(jwt.getSubject()));
 }
}
