package kz.edu.soccerhub.payments.api;

import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.payments.application.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/contracts")
@RequiredArgsConstructor
public class AdminContractPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping("/{contractId}/payment-summary")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ContractPaymentSummaryOutput> getPaymentSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID contractId
    ) {
        return ResponseEntity.ok(adminPaymentService.getContractSummary(getCurrentUserId(jwt), contractId));
    }

    @GetMapping("/{contractId}/payments")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<PaymentOutput>> getContractPayments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID contractId
    ) {
        return ResponseEntity.ok(adminPaymentService.getContractPayments(getCurrentUserId(jwt), contractId));
    }

    private UUID getCurrentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
