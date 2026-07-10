package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.dto.student.AdminStudentDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentRiskCode;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentsPageOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentsQuery;
import kz.edu.soccerhub.admin.application.service.AdminStudentReadService;
import kz.edu.soccerhub.client.application.StudentAvatarService;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.dto.media.MediaDownloadUrlResponse;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentReadService adminStudentReadService;
    private final StudentAvatarService studentAvatarService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AdminStudentsPageOutput> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ContractPaymentStatus paymentStatus,
            @RequestParam(required = false) ContractStatus contractStatus,
            @RequestParam(required = false) AdminStudentRiskCode risk,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminStudentReadService.getStudents(
                UUID.fromString(jwt.getSubject()),
                new AdminStudentsQuery(branchId, search, paymentStatus, contractStatus, risk, groupId),
                pageable,
                sort
        ));
    }

    @GetMapping("/{playerId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AdminStudentDetailsOutput> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID playerId
    ) {
        return ResponseEntity.ok(adminStudentReadService.getStudent(UUID.fromString(jwt.getSubject()), playerId));
    }

    @PostMapping("/{playerId}/avatar")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MediaAssetResponse> uploadAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID playerId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(studentAvatarService.uploadAvatar(
                UUID.fromString(jwt.getSubject()),
                playerId,
                file
        ));
    }

    @DeleteMapping("/{playerId}/avatar")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID playerId
    ) {
        studentAvatarService.deleteAvatar(UUID.fromString(jwt.getSubject()), playerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{playerId}/avatar/download-url")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MediaDownloadUrlResponse> getAvatarDownloadUrl(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID playerId
    ) {
        return ResponseEntity.ok(studentAvatarService.getAvatarDownloadUrl(
                UUID.fromString(jwt.getSubject()),
                playerId
        ));
    }
}
