package kz.edu.soccerhub.dispatcher.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.dispatcher.application.dto.branch.DispatcherBranchCreateInput;
import kz.edu.soccerhub.dispatcher.application.dto.branch.DispatcherBranchUpdateInput;
import kz.edu.soccerhub.dispatcher.application.dto.branch.DispatcherBranchesOutput;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherBranchService;
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
                                          @Valid @RequestBody DispatcherBranchCreateInput input) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        var output = dispatcherBranchService.createBranch(dispatcherId, input);
        return ResponseEntity
                .created(URI.create("/branch/" + output.branchId()))
                .build();
    }


    @GetMapping
    public ResponseEntity<?> getBranches(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        List<DispatcherBranchesOutput> dispatcherBranches = dispatcherBranchService.getDispatcherBranches(dispatcherId).stream()
                .filter(branch -> includeArchived || branch.active()).toList();
        return ResponseEntity.ok().body(
                Map.of("branches", dispatcherBranches)
        );
    }

    @PutMapping("/{branchId}")
    public ResponseEntity<Void> updateBranch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId,
            @Valid @RequestBody DispatcherBranchUpdateInput input) {
        dispatcherBranchService.updateBranch(UUID.fromString(jwt.getSubject()), branchId, input);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{branchId}/archive")
    public ResponseEntity<Void> archiveBranch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId) {
        dispatcherBranchService.archiveBranch(UUID.fromString(jwt.getSubject()), branchId, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{branchId}/restore")
    public ResponseEntity<Void> restoreBranch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId) {
        dispatcherBranchService.archiveBranch(UUID.fromString(jwt.getSubject()), branchId, false);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{branchId}")
    public ResponseEntity<Void> deleteBranch(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID branchId) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        dispatcherBranchService.deleteBranch(dispatcherId, branchId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{branchId}/history")
    public org.springframework.data.domain.Page<kz.edu.soccerhub.common.dto.branch.BranchHistoryOutput> history(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID branchId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return dispatcherBranchService.history(UUID.fromString(jwt.getSubject()),branchId,page,size);
    }
}
