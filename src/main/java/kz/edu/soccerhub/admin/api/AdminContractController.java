package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.service.AdminContractService;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.contract.ContractCancelCommand;
import kz.edu.soccerhub.common.dto.contract.ContractCreateCommand;
import kz.edu.soccerhub.common.dto.contract.ContractDetailsOutput;
import kz.edu.soccerhub.common.dto.contract.ContractExtendCommand;
import kz.edu.soccerhub.common.dto.contract.ContractGroupLookupOutput;
import kz.edu.soccerhub.common.dto.contract.ContractParticipantLookupOutput;
import kz.edu.soccerhub.common.dto.contract.ContractSearchQuery;
import kz.edu.soccerhub.common.dto.contract.ContractUpdateCommand;
import kz.edu.soccerhub.common.dto.contract.ContractsPageOutput;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/admin/contracts")
@RequiredArgsConstructor
public class AdminContractController {

    private final AdminContractService adminContractService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ContractsPageOutput> getContracts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) Set<ContractStatus> status,
            @RequestParam(required = false) LeadType leadType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminContractService.getContracts(
                UUID.fromString(jwt.getSubject()),
                new ContractSearchQuery(branchId, clientId, status, leadType, search),
                pageable
        ));
    }

    @GetMapping("/{contractId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ContractDetailsOutput> getContract(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID contractId
    ) {
        return ResponseEntity.ok(adminContractService.getContract(UUID.fromString(jwt.getSubject()), contractId));
    }

    @GetMapping("/lookups/participants")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ContractParticipantLookupOutput>> getParticipants(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam(required = false) UUID clientId
    ) {
        return ResponseEntity.ok(adminContractService.getParticipants(
                UUID.fromString(jwt.getSubject()), branchId, clientId
        ));
    }

    @GetMapping("/lookups/groups")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ContractGroupLookupOutput>> getGroups(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LeadType leadType
    ) {
        return ResponseEntity.ok(adminContractService.getGroups(UUID.fromString(jwt.getSubject()), branchId, leadType));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractDetailsOutput create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ContractCreateCommand command
    ) {
        return adminContractService.create(UUID.fromString(jwt.getSubject()), command);
    }

    @PatchMapping("/{contractId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ContractDetailsOutput> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID contractId,
            @RequestBody ContractUpdateCommand command
    ) {
        return ResponseEntity.ok(adminContractService.update(UUID.fromString(jwt.getSubject()), contractId, command));
    }

    @PostMapping("/{contractId}/extend")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ContractDetailsOutput> extend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID contractId,
            @RequestBody ContractExtendCommand command
    ) {
        return ResponseEntity.ok(adminContractService.extend(UUID.fromString(jwt.getSubject()), contractId, command));
    }

    @PostMapping("/{contractId}/cancel")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ContractDetailsOutput> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID contractId,
            @RequestBody ContractCancelCommand command
    ) {
        return ResponseEntity.ok(adminContractService.cancel(UUID.fromString(jwt.getSubject()), contractId, command));
    }
}
