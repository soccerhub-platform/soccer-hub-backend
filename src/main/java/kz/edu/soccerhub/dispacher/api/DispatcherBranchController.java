package kz.edu.soccerhub.dispacher.api;

import kz.edu.soccerhub.dispacher.application.dto.branch.DispatcherBranchCreateInput;
import kz.edu.soccerhub.dispacher.application.dto.branch.DispatcherBranchesOutput;
import kz.edu.soccerhub.dispacher.application.service.DispatcherBranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/dispatcher/branch")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DISPATCHER')")
public class DispatcherBranchController {

    private final DispatcherBranchService dispatcherBranchService;

    @PostMapping(value = "/create")
    public ResponseEntity<Void> createBranch(@AuthenticationPrincipal Jwt jwt,
                                          @RequestBody DispatcherBranchCreateInput input) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        var output = dispatcherBranchService.createBranch(dispatcherId, input);
        return ResponseEntity
                .created(URI.create("/branch/" + output.branchId()))
                .build();
    }


    @GetMapping
    public ResponseEntity<?> getBranches(@AuthenticationPrincipal Jwt jwt) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        List<DispatcherBranchesOutput> dispatcherBranches = dispatcherBranchService.getDispatcherBranches(dispatcherId);
        return ResponseEntity.ok().body(
                Map.of("branches", dispatcherBranches)
        );
    }

    @DeleteMapping(value = "/{branchId}")
    public ResponseEntity<Void> deleteBranch(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID branchId) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        dispatcherBranchService.deleteBranch(dispatcherId, branchId);
        return ResponseEntity.noContent().build();
    }
}
