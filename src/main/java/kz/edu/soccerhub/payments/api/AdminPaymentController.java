package kz.edu.soccerhub.payments.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.common.dto.payment.PaymentCancelCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentSearchQuery;
import kz.edu.soccerhub.common.dto.payment.PaymentsPageOutput;
import kz.edu.soccerhub.payments.application.AdminPaymentService;
import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentCreateOutput create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid PaymentCreateCommand command
    ) {
        return adminPaymentService.create(getCurrentUserId(jwt), command);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PaymentsPageOutput> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) Set<PaymentStatus> status,
            @RequestParam(required = false) Set<PaymentMethod> method,
            @RequestParam(required = false) LocalDateTime paidFrom,
            @RequestParam(required = false) LocalDateTime paidTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt", "createdAt"));
        return ResponseEntity.ok(adminPaymentService.list(
                getCurrentUserId(jwt),
                new PaymentSearchQuery(branchId, contractId, clientId, status, method, paidFrom, paidTo),
                pageable
        ));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PaymentOutput> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID paymentId
    ) {
        return ResponseEntity.ok(adminPaymentService.get(getCurrentUserId(jwt), paymentId));
    }

    @PostMapping("/{paymentId}/cancel")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PaymentOutput> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID paymentId,
            @RequestBody @Valid PaymentCancelCommand command
    ) {
        return ResponseEntity.ok(adminPaymentService.cancel(getCurrentUserId(jwt), paymentId, command));
    }

    private UUID getCurrentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
