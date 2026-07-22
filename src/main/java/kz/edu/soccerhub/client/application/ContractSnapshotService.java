package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.contract.StudentContractSnapshotOutput;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.ContractSnapshotPort;
import kz.edu.soccerhub.common.port.GroupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractSnapshotService implements ContractSnapshotPort {

    private final ContractRepository contractRepository;
    private final PlayerRepository playerRepository;
    private final ClientRepository clientRepository;
    private final GroupPort groupPort;
    private final CoachPort coachPort;

    @Override
    @Transactional(readOnly = true)
    public List<StudentContractSnapshotOutput> getStudentContracts(UUID branchId, Collection<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }
        return toSnapshots(
                contractRepository.findByPlayerIdIn(playerIds).stream()
                        .filter(contract -> belongsToBranch(contract, branchId))
                        .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentContractSnapshotOutput> getStudentContracts(UUID branchId, UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        return toSnapshots(
                contractRepository.findByPlayerId(playerId).stream()
                        .filter(contract -> belongsToBranch(contract, branchId))
                        .toList()
        );
    }

    private List<StudentContractSnapshotOutput> toSnapshots(List<Contract> contracts) {
        if (contracts.isEmpty()) {
            return List.of();
        }

        touchLifecycleStatuses(contracts);
        Map<UUID, Player> players = playerRepository.findByIdIn(
                        contracts.stream().map(Contract::getPlayerId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Player::getId, item -> item));
        Set<UUID> clientIds = contracts.stream()
                .map(Contract::getClientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Client> clients = clientRepository.findAllById(clientIds).stream()
                .collect(Collectors.toMap(Client::getId, item -> item));
        Set<UUID> groupIds = contracts.stream()
                .map(Contract::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, GroupDto> groups = groupIds.isEmpty()
                ? Map.of()
                : groupPort.getGroupsByIds(groupIds).stream()
                        .collect(Collectors.toMap(GroupDto::groupId, item -> item));
        Set<UUID> coachIds = contracts.stream()
                .map(Contract::getCoachId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, CoachDto> coaches = coachIds.isEmpty()
                ? Map.of()
                : coachPort.getCoaches(coachIds).stream().collect(Collectors.toMap(CoachDto::id, item -> item));

        return contracts.stream()
                .map(contract -> {
                    Player player = players.get(contract.getPlayerId());
                    Client client = resolveClient(contract, player, clients);
                    GroupDto group = contract.getGroupId() == null ? null : groups.get(contract.getGroupId());
                    CoachDto coach = contract.getCoachId() == null ? null : coaches.get(contract.getCoachId());
                    return new StudentContractSnapshotOutput(
                            contract.getId(),
                            contract.getPlayerId(),
                            client == null ? null : client.getBranchId(),
                            contract.getContractNumber(),
                            contract.getStatus(),
                            contract.getStartDate(),
                            contract.getEndDate(),
                            contract.getAmount(),
                            contract.getCurrency(),
                            contract.getGroupId(),
                            group == null ? null : group.name(),
                            contract.getCoachId(),
                            coach == null ? null : joinName(coach.firstName(), coach.lastName())
                    );
                })
                .toList();
    }

    private boolean belongsToBranch(Contract contract, UUID branchId) {
        if (contract.getClientId() == null) {
            throw new IllegalStateException("Contract has no explicit client: " + contract.getId());
        }
        return clientRepository.findById(contract.getClientId())
                .map(Client::getBranchId)
                .filter(resolvedBranchId -> Objects.equals(resolvedBranchId, branchId))
                .isPresent();
    }

    private Client resolveClient(Contract contract, Player player, Map<UUID, Client> clients) {
        return contract.getClientId() == null ? null : clients.get(contract.getClientId());
    }

    private void touchLifecycleStatuses(Collection<Contract> contracts) {
        contracts.forEach(this::touchLifecycleStatus);
    }

    private void touchLifecycleStatus(Contract contract) {
        if (contract.getStatus() == ContractStatus.CANCELLED
                || contract.getStatus() == ContractStatus.DRAFT) {
            return;
        }
        contract.setStatus(resolveLifecycleStatus(contract));
    }

    private ContractStatus resolveLifecycleStatus(Contract contract) {
        LocalDate today = LocalDate.now();
        if (contract.getEndDate() != null && contract.getEndDate().isBefore(today)) {
            return ContractStatus.EXPIRED;
        }
        if (contract.getStartDate() != null && contract.getStartDate().isAfter(today)) {
            return ContractStatus.UPCOMING;
        }
        return ContractStatus.ACTIVE;
    }

    private String joinName(String firstName, String lastName) {
        String left = firstName == null ? "" : firstName.trim();
        String right = lastName == null ? "" : lastName.trim();
        String full = (left + " " + right).trim();
        return full.isBlank() ? null : full;
    }
}
