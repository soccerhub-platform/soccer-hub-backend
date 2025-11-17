package kz.edu.soccerhub.dispacher.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.dispacher.application.dto.*;
import kz.edu.soccerhub.dispacher.application.service.DispatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dispatcher")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DISPATCHER')")
public class DispatcherController {

    private final DispatcherService dispatcherService;

    @PostMapping(value = "/register")
    public ResponseEntity<?> create(@RequestBody @Valid DispatcherRegisterInput input) {
        DispatcherRegisterOutput output = dispatcherService.register(input);
        return ResponseEntity
                .created(URI.create("/dispatcher/" + output.userId()))
                .body(Map.of("tempPassword", output.tempPassword()));
    }

    @PostMapping(value = "/register/admin")
    public ResponseEntity<?> registerAdmin(@RequestBody DispatcherAdminRegisterInput input) {
       DispatcherAdminRegisterOutput output = dispatcherService.registerAdmin(input);
       return ResponseEntity
               .created(URI.create("/admin/" + output.userId()))
               .body(Map.of("tempPassword", output.tempPassword()));
    }

    @PostMapping(value = "/register/client")
    public ResponseEntity<?> registerClient(@RequestBody DispatcherClientRegisterInput input) {
        var output = dispatcherService.registerClient(input);
        return ResponseEntity
                .created(URI.create("/client/" + output.clientId()))
                .build();
    }

    @PostMapping(value = "/club/create")
    public ResponseEntity<?> createClub(@AuthenticationPrincipal Jwt jwt,
                                        @RequestBody @Valid DispatcherClubCreateInput input) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        var output = dispatcherService.createClub(dispatcherId, input);
        return ResponseEntity
                .created(URI.create("/club/" + output.clubId()))
                .build();
    }

    @GetMapping("/clubs")
    public DispatcherClubsOutput getClubs(@AuthenticationPrincipal Jwt jwt) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return dispatcherService.getDispatcherClubs(dispatcherId);
    }

    @PostMapping(value = "/branch/create")
    public ResponseEntity<?> createBranch(@AuthenticationPrincipal Jwt jwt,
                                          @RequestBody DispatcherBranchCreateInput input) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        var output = dispatcherService.createBranch(dispatcherId, input);
        return ResponseEntity
                .created(URI.create("/branch/" + output.branchId()))
                .build();
    }

    @GetMapping("/branches")
    public DispatcherBranchesOutput getBranches(@AuthenticationPrincipal Jwt jwt) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return dispatcherService.getDispatcherBranches(dispatcherId);
    }
}
