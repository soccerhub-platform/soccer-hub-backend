package kz.edu.soccerhub.common.port;

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
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ContractPort {

    ContractsPageOutput search(ContractSearchQuery query, Pageable pageable);

    ContractDetailsOutput getById(UUID contractId);

    UUID getBranchId(UUID contractId);

    List<ContractParticipantLookupOutput> getParticipants(UUID branchId);

    List<ContractGroupLookupOutput> getGroups(UUID branchId, LeadType leadType);

    ContractDetailsOutput create(ContractCreateCommand command, UUID actorUserId);

    ContractDetailsOutput update(UUID contractId, ContractUpdateCommand command, UUID actorUserId);

    ContractDetailsOutput extend(UUID contractId, ContractExtendCommand command, UUID actorUserId);

    ContractDetailsOutput cancel(UUID contractId, ContractCancelCommand command, UUID actorUserId);
}
