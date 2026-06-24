package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.contract.StudentContractSnapshotOutput;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ContractSnapshotPort {

    List<StudentContractSnapshotOutput> getStudentContracts(UUID branchId, Collection<UUID> playerIds);

    List<StudentContractSnapshotOutput> getStudentContracts(UUID branchId, UUID playerId);
}
