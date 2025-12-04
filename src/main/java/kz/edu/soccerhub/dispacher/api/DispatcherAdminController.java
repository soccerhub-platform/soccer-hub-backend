package kz.edu.soccerhub.dispacher.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.dispacher.application.dto.admin.*;
import kz.edu.soccerhub.dispacher.application.service.DispatcherAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dispatcher/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DISPATCHER')")
public class DispatcherAdminController {

    private final DispatcherAdminService dispatcherAdminService;

    @PostMapping(value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerAdmin(@RequestBody DispatcherAdminRegisterInput input) {
        DispatcherAdminRegisterOutput output = dispatcherAdminService.registerAdmin(input);
        return ResponseEntity
                .created(URI.create("/admin/" + output.userId()))
                .body(Map.of("tempPassword", output.tempPassword()));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAdmins(@AuthenticationPrincipal Jwt jwt) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        dispatcherAdminService.getAdmins(dispatcherId);
        return ResponseEntity.ok().body(
                Map.of("admins", dispatcherAdminService.getAdmins(dispatcherId))
        );
    }

    @DeleteMapping(value = "/{adminId}")
    public ResponseEntity<Void> deleteAdmin(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable UUID adminId) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        dispatcherAdminService.deleteAdmin(dispatcherId, adminId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{adminId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> changeAdminStatus(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable UUID adminId,
                                                  @Valid @RequestBody DispatcherAdminChangeStatusInput input) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        dispatcherAdminService.changeAdminStatus(dispatcherId, adminId, input);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{adminId}/assign-branch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> assignAdminToBranch(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable UUID adminId,
                                                    @Valid @RequestBody DispatcherAdminAssignBranchInput input) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        dispatcherAdminService.assignAdminToBranch(dispatcherId, adminId, input);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{adminId}/unassign-branch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> unassignAdminFromBranch(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable UUID adminId,
                                                        @Valid @RequestBody DispatcherAdminUnAssignBranchInput input) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        dispatcherAdminService.unassignAdminFromBranch(dispatcherId, adminId, input);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{adminId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateAdmin(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable UUID adminId,
                                            @Valid @RequestBody DispatcherAdminUpdateInput input) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        dispatcherAdminService.updateAdmin(dispatcherId, adminId, input);
        return ResponseEntity.noContent().build();
    }

}
