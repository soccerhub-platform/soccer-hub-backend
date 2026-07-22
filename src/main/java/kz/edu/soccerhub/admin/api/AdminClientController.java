package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.service.AdminClientReadService;
import kz.edu.soccerhub.admin.application.service.AdminClientWriteService;
import kz.edu.soccerhub.admin.application.service.AdminClientActivityService;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientActivityOutput;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientCreateInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientUpdateInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientStatusInput;
import jakarta.validation.Valid;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceDetailsOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceListQuery;
import kz.edu.soccerhub.common.dto.client.ClientWorkspacePageOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;
import java.util.Set;

@RestController
@RequestMapping("/admin/clients")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminClientController {

    private final AdminClientReadService clientReadService;
    private final AdminClientWriteService clientWriteService;
    private final AdminClientActivityService clientActivityService;

    @GetMapping
    public ResponseEntity<ClientWorkspacePageOutput> getClients(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Set<String> status,
            @RequestParam(required = false) String students,
            @RequestParam(required = false) String contracts,
            @RequestParam(required = false) String payment,
            @PageableDefault(size = 20, sort = "firstName") Pageable pageable
    ) {
        return ResponseEntity.ok(clientReadService.getClients(
                adminId(jwt), branchId,
                new ClientWorkspaceListQuery(search, status, students, contracts, payment),
                pageable
        ));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientWorkspaceDetailsOutput> getClient(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clientId
    ) {
        return ResponseEntity.ok(clientReadService.getClient(adminId(jwt), clientId));
    }

    @GetMapping("/{clientId}/activity")
    public ResponseEntity<org.springframework.data.domain.Page<AdminClientActivityOutput>> getActivity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clientId,
            @PageableDefault(size = 20, sort = "occurredAt") Pageable pageable
    ) {
        return ResponseEntity.ok(clientActivityService.getActivity(adminId(jwt), clientId, pageable));
    }

    @PostMapping
    public ResponseEntity<ClientWorkspaceDetailsOutput> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminClientCreateInput input
    ) {
        return ResponseEntity.ok(clientWriteService.create(adminId(jwt), input));
    }

    @PatchMapping("/{clientId}")
    public ResponseEntity<ClientWorkspaceDetailsOutput> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clientId,
            @Valid @RequestBody AdminClientUpdateInput input
    ) {
        return ResponseEntity.ok(clientWriteService.update(adminId(jwt), clientId, input));
    }

    @PatchMapping("/{clientId}/status")
    public ResponseEntity<ClientWorkspaceDetailsOutput> changeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clientId,
            @Valid @RequestBody AdminClientStatusInput input
    ) {
        return ResponseEntity.ok(clientWriteService.changeStatus(adminId(jwt), clientId, input));
    }

    private UUID adminId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
