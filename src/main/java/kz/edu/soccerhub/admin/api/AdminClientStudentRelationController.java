package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.dto.client.AdminCreateClientStudentRelationInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminCreateClientStudentInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminEndClientStudentRelationInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminUpdateClientStudentRelationInput;
import kz.edu.soccerhub.admin.application.service.AdminClientStudentRelationService;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminClientStudentRelationController {

    private final AdminClientStudentRelationService relationService;

    @GetMapping("/clients/{clientId}/students")
    public ResponseEntity<List<ClientStudentRelationOutput>> getClientStudents(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clientId
    ) {
        return ResponseEntity.ok(relationService.getClientStudents(adminId(jwt), clientId));
    }

    @GetMapping("/students/{playerId}/clients")
    public ResponseEntity<List<ClientStudentRelationOutput>> getStudentClients(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID playerId
    ) {
        return ResponseEntity.ok(relationService.getStudentClients(adminId(jwt), playerId));
    }

    @PostMapping("/clients/{clientId}/students")
    public ResponseEntity<ClientStudentRelationOutput> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clientId,
            @Valid @RequestBody AdminCreateClientStudentRelationInput input
    ) {
        return ResponseEntity.ok(relationService.create(adminId(jwt), clientId, input));
    }

    @PostMapping("/clients/{clientId}/students/create")
    public ResponseEntity<ClientStudentRelationOutput> createStudent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clientId,
            @Valid @RequestBody AdminCreateClientStudentInput input
    ) {
        return ResponseEntity.ok(relationService.createStudent(adminId(jwt), clientId, input));
    }

    @PatchMapping("/client-student-relations/{relationId}")
    public ResponseEntity<ClientStudentRelationOutput> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID relationId,
            @Valid @RequestBody AdminUpdateClientStudentRelationInput input
    ) {
        return ResponseEntity.ok(relationService.update(adminId(jwt), relationId, input));
    }

    @PostMapping("/client-student-relations/{relationId}/end")
    public ResponseEntity<ClientStudentRelationOutput> end(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID relationId,
            @Valid @RequestBody AdminEndClientStudentRelationInput input
    ) {
        return ResponseEntity.ok(relationService.end(adminId(jwt), relationId, input));
    }

    private UUID adminId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
