package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.common.dto.contract.ContractCancelCommand;
import kz.edu.soccerhub.common.dto.contract.ContractCreateCommand;
import kz.edu.soccerhub.common.dto.contract.ContractDetailsOutput;
import kz.edu.soccerhub.common.dto.contract.ContractExtendCommand;
import kz.edu.soccerhub.common.dto.contract.ContractGroupLookupOutput;
import kz.edu.soccerhub.common.dto.contract.ContractParticipantLookupOutput;
import kz.edu.soccerhub.common.dto.contract.ContractSearchQuery;
import kz.edu.soccerhub.common.dto.contract.ContractUpdateCommand;
import kz.edu.soccerhub.common.dto.contract.ContractsPageOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ContractPort;
import kz.edu.soccerhub.common.port.PaymentPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminContractService {

    private final ContractPort contractPort;
    private final PaymentPort paymentPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    @Transactional(readOnly = true)
    public ContractsPageOutput getContracts(UUID adminId, ContractSearchQuery query, Pageable pageable) {
        verifyAdminAccessToBranch(adminId, query.branchId());
        return enrich(contractPort.search(query, pageable));
    }

    @Transactional(readOnly = true)
    public ContractDetailsOutput getContract(UUID adminId, UUID contractId) {
        verifyAdminAccessToBranch(adminId, contractPort.getBranchId(contractId));
        return enrich(contractPort.getById(contractId));
    }

    @Transactional(readOnly = true)
    public List<ContractParticipantLookupOutput> getParticipants(UUID adminId, UUID branchId, UUID clientId) {
        verifyAdminAccessToBranch(adminId, branchId);
        return contractPort.getParticipants(branchId, clientId);
    }

    @Transactional(readOnly = true)
    public List<ContractGroupLookupOutput> getGroups(UUID adminId, UUID branchId, LeadType leadType) {
        verifyAdminAccessToBranch(adminId, branchId);
        return contractPort.getGroups(branchId, leadType);
    }

    @Transactional
    public ContractDetailsOutput create(UUID adminId, ContractCreateCommand command) {
        verifyAdminAccessToBranch(adminId, command.branchId());
        return enrich(contractPort.create(command, adminId));
    }

    @Transactional
    public ContractDetailsOutput update(UUID adminId, UUID contractId, ContractUpdateCommand command) {
        UUID currentBranchId = contractPort.getBranchId(contractId);
        verifyAdminAccessToBranch(adminId, currentBranchId);
        return enrich(contractPort.update(contractId, command, adminId));
    }

    @Transactional
    public ContractDetailsOutput activate(UUID adminId, UUID contractId) {
        verifyAdminAccessToBranch(adminId, contractPort.getBranchId(contractId));
        return enrich(contractPort.activate(contractId, adminId));
    }

    @Transactional
    public ContractDetailsOutput extend(UUID adminId, UUID contractId, ContractExtendCommand command) {
        verifyAdminAccessToBranch(adminId, contractPort.getBranchId(contractId));
        return enrich(contractPort.extend(contractId, command, adminId));
    }

    @Transactional
    public ContractDetailsOutput cancel(UUID adminId, UUID contractId, ContractCancelCommand command) {
        verifyAdminAccessToBranch(adminId, contractPort.getBranchId(contractId));
        return enrich(contractPort.cancel(contractId, command, adminId));
    }

    /**
     * Contract read responses are enriched above the contract module so payment
     * calculations stay owned by the payments module without introducing a
     * bidirectional dependency between contract and payments services.
     */
    private ContractsPageOutput enrich(ContractsPageOutput page) {
        Map<UUID, ContractPaymentSummaryOutput> summaries = paymentPort.getContractPaymentSummaries(
                page.content().stream()
                        .map(item -> new ContractPaymentSummaryQueryInput(item.id(), item.amount()))
                        .toList()
        );

        List<kz.edu.soccerhub.common.dto.contract.ContractListItemOutput> enrichedContent = page.content().stream()
                .map(item -> enrich(item, summaries.get(item.id())))
                .toList();

        return new ContractsPageOutput(
                enrichedContent,
                page.totalElements(),
                page.totalPages(),
                page.number(),
                page.size()
        );
    }

    private ContractDetailsOutput enrich(ContractDetailsOutput details) {
        ContractPaymentSummaryOutput summary = paymentPort.getContractPaymentSummaries(
                List.of(new ContractPaymentSummaryQueryInput(details.id(), details.amount()))
        ).get(details.id());

        return new ContractDetailsOutput(
                details.id(),
                details.contractNumber(),
                details.branchId(),
                details.leadType(),
                details.status(),
                details.amount(),
                details.currency(),
                details.startDate(),
                details.endDate(),
                details.notes(),
                details.participant(),
                details.primaryContact(),
                details.group(),
                details.coach(),
                summary.paymentStatus(),
                summary.paidAmount(),
                summary.outstandingAmount(),
                summary.overpaidAmount(),
                summary.lastPaidAt(),
                details.createdAt(),
                details.updatedAt(),
                details.history(),
                details.capabilities()
        );
    }

    private kz.edu.soccerhub.common.dto.contract.ContractListItemOutput enrich(
            kz.edu.soccerhub.common.dto.contract.ContractListItemOutput item,
            ContractPaymentSummaryOutput summary
    ) {
        return new kz.edu.soccerhub.common.dto.contract.ContractListItemOutput(
                item.id(),
                item.contractNumber(),
                item.branchId(),
                item.leadType(),
                item.status(),
                item.amount(),
                item.currency(),
                item.startDate(),
                item.endDate(),
                item.notes(),
                item.participant(),
                item.primaryContact(),
                item.group(),
                item.coach(),
                summary.paymentStatus(),
                summary.paidAmount(),
                summary.outstandingAmount(),
                summary.overpaidAmount(),
                summary.lastPaidAt(),
                item.createdAt(),
                item.updatedAt()
        );
    }

    private void verifyAdminAccessToBranch(UUID adminId, UUID branchId) {
        verifyAdminExists(adminId);
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }

    private void verifyAdminExists(UUID adminId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
    }
}
