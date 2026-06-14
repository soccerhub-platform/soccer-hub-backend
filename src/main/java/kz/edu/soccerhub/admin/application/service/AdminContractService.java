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
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ContractPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminContractService {

    private final ContractPort contractPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    @Transactional(readOnly = true)
    public ContractsPageOutput getContracts(UUID adminId, ContractSearchQuery query, Pageable pageable) {
        verifyAdminAccessToBranch(adminId, query.branchId());
        return contractPort.search(query, pageable);
    }

    @Transactional(readOnly = true)
    public ContractDetailsOutput getContract(UUID adminId, UUID contractId) {
        verifyAdminAccessToBranch(adminId, contractPort.getBranchId(contractId));
        return contractPort.getById(contractId);
    }

    @Transactional(readOnly = true)
    public List<ContractParticipantLookupOutput> getParticipants(UUID adminId, UUID branchId) {
        verifyAdminAccessToBranch(adminId, branchId);
        return contractPort.getParticipants(branchId);
    }

    @Transactional(readOnly = true)
    public List<ContractGroupLookupOutput> getGroups(UUID adminId, UUID branchId, LeadType leadType) {
        verifyAdminAccessToBranch(adminId, branchId);
        return contractPort.getGroups(branchId, leadType);
    }

    @Transactional
    public ContractDetailsOutput create(UUID adminId, ContractCreateCommand command) {
        verifyAdminAccessToBranch(adminId, command.branchId());
        return contractPort.create(command, adminId);
    }

    @Transactional
    public ContractDetailsOutput update(UUID adminId, UUID contractId, ContractUpdateCommand command) {
        UUID currentBranchId = contractPort.getBranchId(contractId);
        verifyAdminAccessToBranch(adminId, currentBranchId);
        if (command.branchId() != null && !Objects.equals(command.branchId(), currentBranchId)) {
            verifyAdminAccessToBranch(adminId, command.branchId());
        }
        return contractPort.update(contractId, command, adminId);
    }

    @Transactional
    public ContractDetailsOutput extend(UUID adminId, UUID contractId, ContractExtendCommand command) {
        verifyAdminAccessToBranch(adminId, contractPort.getBranchId(contractId));
        return contractPort.extend(contractId, command, adminId);
    }

    @Transactional
    public ContractDetailsOutput cancel(UUID adminId, UUID contractId, ContractCancelCommand command) {
        verifyAdminAccessToBranch(adminId, contractPort.getBranchId(contractId));
        return contractPort.cancel(contractId, command, adminId);
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
