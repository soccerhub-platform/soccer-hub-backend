package kz.edu.soccerhub.admin.api;


import kz.edu.soccerhub.admin.application.dto.branch.AdminBranchesOutput;
import kz.edu.soccerhub.admin.application.service.AdminBranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/branch")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminBranchController {

    private final AdminBranchService adminBranchService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAdminBranches(@AuthenticationPrincipal Jwt jwt) {
        final UUID adminId = UUID.fromString(jwt.getSubject());
        Collection<AdminBranchesOutput> adminBranches = adminBranchService.getAdminBranches(adminId);
        return ResponseEntity.ok().body(
                Map.of("branches", adminBranches)
        );
    }

}
