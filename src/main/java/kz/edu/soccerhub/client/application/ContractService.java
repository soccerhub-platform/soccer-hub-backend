package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.client.domain.enums.ContractHistoryType;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.ContractHistory;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ContractHistoryRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.contract.ContractCancelCommand;
import kz.edu.soccerhub.common.dto.contract.ContractCoachOutput;
import kz.edu.soccerhub.common.dto.contract.ContractCreateCommand;
import kz.edu.soccerhub.common.dto.contract.ContractDetailsOutput;
import kz.edu.soccerhub.common.dto.contract.ContractExtendCommand;
import kz.edu.soccerhub.common.dto.contract.ContractGroupLookupOutput;
import kz.edu.soccerhub.common.dto.contract.ContractGroupOutput;
import kz.edu.soccerhub.common.dto.contract.ContractHistoryOutput;
import kz.edu.soccerhub.common.dto.contract.ContractListItemOutput;
import kz.edu.soccerhub.common.dto.contract.ContractParticipantDraftInput;
import kz.edu.soccerhub.common.dto.contract.ContractParticipantLookupOutput;
import kz.edu.soccerhub.common.dto.contract.ContractParticipantOutput;
import kz.edu.soccerhub.common.dto.contract.ContractPrimaryContactDraftInput;
import kz.edu.soccerhub.common.dto.contract.ContractPrimaryContactOutput;
import kz.edu.soccerhub.common.dto.contract.ContractSearchQuery;
import kz.edu.soccerhub.common.dto.contract.StudentContractSnapshotOutput;
import kz.edu.soccerhub.common.dto.contract.ContractUpdateCommand;
import kz.edu.soccerhub.common.dto.contract.ContractValidationError;
import kz.edu.soccerhub.common.dto.contract.ContractsPageOutput;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentContextOutput;
import kz.edu.soccerhub.common.exception.ContractValidationException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.ContractPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.organization.application.service.GroupMembershipSyncService;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService implements ContractPort {

    private static final String DEFAULT_CURRENCY = "KZT";

    private final ContractRepository contractRepository;
    private final ContractHistoryRepository contractHistoryRepository;
    private final PlayerRepository playerRepository;
    private final ClientRepository clientRepository;
    private final GroupPort groupPort;
    private final GroupCoachPort groupCoachPort;
    private final CoachPort coachPort;
    private final AdminPort adminPort;
    private final AuthPort authPort;
    private final LeadPort leadPort;
    private final GroupMembershipSyncService groupMembershipSyncService;

    @Override
    @Transactional(readOnly = true)
    public ContractsPageOutput search(ContractSearchQuery query, Pageable pageable) {
        Page<Contract> page = contractRepository.search(
                query.branchId(),
                query.leadType(),
                query.statuses() == null ? Set.of() : query.statuses(),
                query.statuses() == null || query.statuses().isEmpty(),
                normalizeSearchLike(query.search()),
                parseUuid(query.search()),
                pageable
        );

        List<Contract> contracts = page.getContent();
        touchLifecycleStatuses(contracts);

        Map<UUID, Player> players = loadPlayers(contracts);
        Map<UUID, Client> clients = loadClients(players.values());
        Map<UUID, GroupDto> groups = loadGroups(contracts);
        Map<UUID, CoachDto> coaches = loadCoaches(contracts);

        List<ContractListItemOutput> items = contracts.stream()
                .map(contract -> toListItem(contract, players.get(contract.getPlayerId()), clients, groups, coaches))
                .toList();

        return new ContractsPageOutput(
                items,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDetailsOutput getById(UUID contractId) {
        Contract contract = findContract(contractId);
        touchLifecycleStatus(contract);

        Player player = findPlayer(contract.getPlayerId());
        Client client = requireParent(player);
        GroupDto group = groupPort.getGroupById(contract.getGroupId());
        CoachDto coach = contract.getCoachId() == null ? null : coachPort.getCoach(contract.getCoachId());

        return toDetails(contract, player, client, group, coach, loadHistory(contractId));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getBranchId(UUID contractId) {
        Player player = findPlayer(findContract(contractId).getPlayerId());
        return requireParent(player).getBranchId();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractPaymentContextOutput getPaymentContext(UUID contractId) {
        Contract contract = findContract(contractId);
        Player player = findPlayer(contract.getPlayerId());
        Client client = requireParent(player);
        touchLifecycleStatus(contract);

        return new ContractPaymentContextOutput(
                contract.getId(),
                contract.getContractNumber(),
                client.getId(),
                buildClientName(client),
                player.getId(),
                buildPlayerName(player),
                client.getBranchId(),
                defaultAmount(contract.getAmount()),
                contract.getCurrency(),
                contract.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractParticipantLookupOutput> getParticipants(UUID branchId) {
        List<Player> players = playerRepository.findAllByParentBranchId(branchId);
        if (players.isEmpty()) {
            return List.of();
        }

        Map<UUID, Contract> latestContracts = new HashMap<>();
        for (Player player : players) {
            contractRepository.findTopByPlayerIdOrderByCreatedAtDesc(player.getId())
                    .ifPresent(contract -> latestContracts.put(player.getId(), contract));
        }
        Map<UUID, LeadType> latestLeadTypes = leadPort.getLatestLeadTypesByParticipantIds(
                players.stream().map(Player::getId).toList()
        );

        return players.stream()
                .map(player -> new ContractParticipantLookupOutput(
                        player.getId(),
                        buildPlayerName(player),
                        player.getBirthDate(),
                        resolveLeadType(player.getId(), latestContracts, latestLeadTypes),
                        toPrimaryContact(requireParent(player))
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractGroupLookupOutput> getGroups(UUID branchId, LeadType leadType) {
        List<GroupDto> groups = groupPort.getGroupsByBranch(branchId).stream()
                .filter(group -> group.status() == GroupStatus.ACTIVE)
                .filter(group -> group.audienceType() != null && group.audienceType().name().equals(leadType.name()))
                .sorted(Comparator.comparing(GroupDto::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<UUID, UUID> coachIdsByGroup = new HashMap<>();
        for (GroupDto group : groups) {
            groupCoachPort.getActiveCoaches(group.groupId()).stream()
                    .sorted(Comparator
                            .comparing((GroupCoachDto item) -> item.role() != CoachRole.MAIN)
                            .thenComparing(GroupCoachDto::assignedFrom, Comparator.nullsLast(Comparator.naturalOrder())))
                    .findFirst()
                    .ifPresent(item -> coachIdsByGroup.put(group.groupId(), item.coachId()));
        }

        Set<UUID> coachIds = new HashSet<>(coachIdsByGroup.values());
        Map<UUID, CoachDto> coaches = coachIds.isEmpty()
                ? Map.of()
                : coachPort.getCoaches(coachIds).stream().collect(Collectors.toMap(CoachDto::id, item -> item));

        return groups.stream()
                .map(group -> new ContractGroupLookupOutput(
                        group.groupId(),
                        group.branchId(),
                        group.name(),
                        group.audienceType(),
                        toCoachOutput(coaches.get(coachIdsByGroup.get(group.groupId())))
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentContractSnapshotOutput> getStudentContracts(UUID branchId, Collection<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }
        return toStudentContractSnapshots(
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
        return toStudentContractSnapshots(
                contractRepository.findByPlayerId(playerId).stream()
                        .filter(contract -> belongsToBranch(contract, branchId))
                        .toList()
        );
    }

    @Override
    @Transactional
    public ContractDetailsOutput create(ContractCreateCommand command, UUID actorUserId) {
        List<ContractValidationError> errors = new ArrayList<>();
        validateCreateCommand(command, errors);

        ResolvedParticipant resolved = resolveCreateParticipant(command, errors);
        GroupDto group = resolveGroup(command.branchId(), command.groupId(), command.leadType(), errors);
        CoachDto coach = resolveCoach(command.coachId(), errors);

        if (resolved.player != null && command.startDate() != null) {
            validateOverlap(null, resolved.player.getId(), command.startDate(), command.endDate(), errors);
        }

        throwIfErrors(errors);

        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .playerId(resolved.player.getId())
                .groupId(group.groupId())
                .contractNumber(resolveContractNumber(command.contractNumber()))
                .leadType(command.leadType())
                .status(ContractStatus.ACTIVE)
                .coachId(command.coachId())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .amount(defaultAmount(command.amount()))
                .currency(resolveCurrency(command.currency()))
                .notes(trimToNull(command.notes()))
                .build();
        contractRepository.save(contract);
        groupMembershipSyncService.syncFromContract(contract);
        appendHistory(contract.getId(), ContractHistoryType.CREATED, actorUserId, command.notes(), "Initial create");

        return toDetails(contract, resolved.player, resolved.client, group, coach, loadHistory(contract.getId()));
    }

    @Override
    @Transactional
    public ContractDetailsOutput update(UUID contractId, ContractUpdateCommand command, UUID actorUserId) {
        Contract contract = findContract(contractId);
        touchLifecycleStatus(contract);
        validateUpdatable(contract);

        List<ContractValidationError> errors = new ArrayList<>();
        validateUpdateCommand(command, contract, errors);

        Player player = command.participantId() == null ? null : playerRepository.findById(command.participantId()).orElse(null);
        Client client = player == null ? null : requireParent(player);
        if (player == null) {
            errors.add(error("REQUIRED", "participantId", "Выберите участника"));
        }
        if (command.primaryContactId() == null) {
            errors.add(error("REQUIRED", "primaryContactId", "Выберите основной контакт"));
        } else if (client != null && !Objects.equals(client.getId(), command.primaryContactId())) {
            errors.add(error("MISMATCH", "primaryContactId", "Контакт не соответствует участнику"));
        }
        if (client != null && command.branchId() != null && !Objects.equals(client.getBranchId(), command.branchId())) {
            errors.add(error("BRANCH_MISMATCH", "participantId", "Участник не относится к выбранному филиалу"));
        }

        GroupDto group = resolveGroup(command.branchId(), command.groupId(), command.leadType(), errors);
        CoachDto coach = resolveCoach(command.coachId(), errors);

        if (player != null && command.startDate() != null) {
            validateOverlap(contract.getId(), player.getId(), command.startDate(), command.endDate(), errors);
        }

        throwIfErrors(errors);

        contract.setPlayerId(player.getId());
        contract.setGroupId(group.groupId());
        contract.setLeadType(command.leadType());
        contract.setCoachId(command.coachId());
        contract.setStartDate(command.startDate());
        contract.setEndDate(command.endDate());
        contract.setAmount(defaultAmount(command.amount()));
        contract.setCurrency(resolveCurrency(command.currency()));
        contract.setNotes(trimToNull(command.notes()));
        touchLifecycleStatus(contract);
        groupMembershipSyncService.syncFromContract(contract);

        appendHistory(contract.getId(), ContractHistoryType.UPDATED, actorUserId, command.notes(), "Contract updated");
        return toDetails(contract, player, client, group, coach, loadHistory(contract.getId()));
    }

    @Override
    @Transactional
    public ContractDetailsOutput extend(UUID contractId, ContractExtendCommand command, UUID actorUserId) {
        Contract contract = findContract(contractId);
        touchLifecycleStatus(contract);
        validateExtendable(contract);

        List<ContractValidationError> errors = new ArrayList<>();
        if (command.endDate() == null) {
            errors.add(error("REQUIRED", "endDate", "Укажите дату окончания"));
        } else if (command.endDate().isBefore(contract.getStartDate())) {
            errors.add(error("INVALID_DATE_RANGE", "endDate", "Дата окончания не может быть раньше даты начала"));
        }
        if (command.amount() != null && command.amount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(error("MIN", "amount", "Сумма не может быть отрицательной"));
        }
        validateOverlap(contract.getId(), contract.getPlayerId(), contract.getStartDate(), command.endDate(), errors);
        throwIfErrors(errors);

        contract.setEndDate(command.endDate());
        if (command.amount() != null) {
            contract.setAmount(command.amount());
        }
        if (command.notes() != null) {
            contract.setNotes(trimToNull(command.notes()));
        }
        touchLifecycleStatus(contract);
        groupMembershipSyncService.syncFromContract(contract);

        Player player = findPlayer(contract.getPlayerId());
        Client client = requireParent(player);
        GroupDto group = groupPort.getGroupById(contract.getGroupId());
        CoachDto coach = contract.getCoachId() == null ? null : coachPort.getCoach(contract.getCoachId());

        appendHistory(contract.getId(), ContractHistoryType.EXTENDED, actorUserId, command.notes(), "Contract extended");
        return toDetails(contract, player, client, group, coach, loadHistory(contract.getId()));
    }

    @Override
    @Transactional
    public ContractDetailsOutput cancel(UUID contractId, ContractCancelCommand command, UUID actorUserId) {
        Contract contract = findContract(contractId);
        touchLifecycleStatus(contract);
        validateUpdatable(contract);

        List<ContractValidationError> errors = new ArrayList<>();
        if (command.reasonCode() == null) {
            errors.add(error("REQUIRED", "reasonCode", "Укажите причину отмены"));
        }
        if (command.reasonCode() != null && command.reasonCode().name().equals("OTHER") && trimToNull(command.comment()) == null) {
            errors.add(error("REQUIRED", "comment", "Комментарий обязателен для причины OTHER"));
        }
        throwIfErrors(errors);

        contract.setStatus(ContractStatus.CANCELLED);
        contract.setCancelReasonCode(command.reasonCode());
        contract.setCancelComment(trimToNull(command.comment()));
        groupMembershipSyncService.syncFromContract(contract);

        Player player = findPlayer(contract.getPlayerId());
        Client client = requireParent(player);
        GroupDto group = groupPort.getGroupById(contract.getGroupId());
        CoachDto coach = contract.getCoachId() == null ? null : coachPort.getCoach(contract.getCoachId());

        appendHistory(contract.getId(), ContractHistoryType.CANCELLED, actorUserId, command.comment(), "Contract cancelled");
        return toDetails(contract, player, client, group, coach, loadHistory(contract.getId()));
    }

    private void validateCreateCommand(ContractCreateCommand command, List<ContractValidationError> errors) {
        if (command.branchId() == null) {
            errors.add(error("REQUIRED", "branchId", "Выберите филиал"));
        }
        if (command.leadType() == null) {
            errors.add(error("REQUIRED", "leadType", "Укажите тип лида"));
        }
        if (command.participantId() != null && command.participantDraft() != null) {
            errors.add(error("CONFLICT", "participantId", "Используйте либо existing participant, либо participantDraft"));
        }
        if (command.startDate() == null) {
            errors.add(error("REQUIRED", "startDate", "Укажите дату начала"));
        }
        if (command.endDate() != null && command.startDate() != null && command.endDate().isBefore(command.startDate())) {
            errors.add(error("INVALID_DATE_RANGE", "endDate", "Дата окончания не может быть раньше даты начала"));
        }
        if (command.amount() != null && command.amount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(error("MIN", "amount", "Сумма не может быть отрицательной"));
        }
    }

    private void validateUpdateCommand(ContractUpdateCommand command, Contract contract, List<ContractValidationError> errors) {
        if (command.branchId() == null) {
            errors.add(error("REQUIRED", "branchId", "Выберите филиал"));
        }
        if (command.leadType() == null) {
            errors.add(error("REQUIRED", "leadType", "Укажите тип лида"));
        }
        if (command.startDate() == null) {
            errors.add(error("REQUIRED", "startDate", "Укажите дату начала"));
        }
        if (command.endDate() != null && command.startDate() != null && command.endDate().isBefore(command.startDate())) {
            errors.add(error("INVALID_DATE_RANGE", "endDate", "Дата окончания не может быть раньше даты начала"));
        }
        if (command.amount() != null && command.amount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(error("MIN", "amount", "Сумма не может быть отрицательной"));
        }
        if (trimToNull(command.contractNumber()) != null && !Objects.equals(trimToNull(command.contractNumber()), contract.getContractNumber())) {
            errors.add(error("IMMUTABLE", "contractNumber", "Номер договора нельзя менять после создания"));
        }
    }

    private ResolvedParticipant resolveCreateParticipant(ContractCreateCommand command, List<ContractValidationError> errors) {
        if (command.participantId() != null) {
            Player player = playerRepository.findById(command.participantId()).orElse(null);
            if (player == null) {
                errors.add(error("NOT_FOUND", "participantId", "Участник не найден"));
                return ResolvedParticipant.empty();
            }
            Client client = requireParent(player);
            if (command.primaryContactId() == null) {
                errors.add(error("REQUIRED", "primaryContactId", "Выберите основной контакт"));
            } else if (!Objects.equals(client.getId(), command.primaryContactId())) {
                errors.add(error("MISMATCH", "primaryContactId", "Контакт не соответствует участнику"));
            }
            if (command.primaryContactDraft() != null) {
                errors.add(error("CONFLICT", "contactFullName", "Нельзя передавать draft-контакт для существующего участника"));
            }
            if (command.branchId() != null && !Objects.equals(client.getBranchId(), command.branchId())) {
                errors.add(error("BRANCH_MISMATCH", "participantId", "Участник не относится к выбранному филиалу"));
            }
            return new ResolvedParticipant(player, client);
        }

        if (command.participantDraft() == null) {
            errors.add(error("REQUIRED", "participantId", "Выберите участника"));
            return ResolvedParticipant.empty();
        }

        Client client = resolvePrimaryContact(command, errors);
        validateParticipantDraft(command.participantDraft(), errors);
        if (client == null || !errors.isEmpty()) {
            return ResolvedParticipant.empty();
        }
        return new ResolvedParticipant(resolveOrCreatePlayer(client, command.participantDraft()), client);
    }

    private Client resolvePrimaryContact(ContractCreateCommand command, List<ContractValidationError> errors) {
        if (command.primaryContactId() != null && command.primaryContactDraft() != null) {
            errors.add(error("CONFLICT", "primaryContactId", "Используйте либо primaryContactId, либо primaryContactDraft"));
            return null;
        }
        if (command.primaryContactId() != null) {
            Client client = clientRepository.findById(command.primaryContactId()).orElse(null);
            if (client == null) {
                errors.add(error("NOT_FOUND", "primaryContactId", "Контакт не найден"));
                return null;
            }
            if (command.branchId() != null && !Objects.equals(client.getBranchId(), command.branchId())) {
                errors.add(error("BRANCH_MISMATCH", "primaryContactId", "Контакт не относится к выбранному филиалу"));
            }
            return client;
        }
        if (command.primaryContactDraft() == null) {
            errors.add(error("REQUIRED", "primaryContactId", "Выберите основной контакт"));
            return null;
        }
        validatePrimaryContactDraft(command.primaryContactDraft(), errors);
        if (!errors.isEmpty()) {
            return null;
        }
        return resolveOrCreateClient(command.branchId(), command.primaryContactDraft());
    }

    private void validatePrimaryContactDraft(ContractPrimaryContactDraftInput draft, List<ContractValidationError> errors) {
        if (trimToNull(draft.fullName()) == null) {
            errors.add(error("REQUIRED", "contactFullName", "Укажите имя контакта"));
        }
        if (trimToNull(draft.phone()) == null) {
            errors.add(error("REQUIRED", "contactPhone", "Укажите телефон контакта"));
        }
    }

    private void validateParticipantDraft(ContractParticipantDraftInput draft, List<ContractValidationError> errors) {
        if (trimToNull(draft.fullName()) == null) {
            errors.add(error("REQUIRED", "participantFullName", "Укажите имя участника"));
        }
        if (draft.birthDate() == null) {
            errors.add(error("REQUIRED", "participantBirthDate", "Укажите дату рождения участника"));
        }
    }

    private GroupDto resolveGroup(UUID branchId, UUID groupId, LeadType leadType, List<ContractValidationError> errors) {
        if (groupId == null) {
            errors.add(error("REQUIRED", "groupId", "Выберите группу"));
            return null;
        }
        GroupDto group = groupPort.getGroupById(groupId);
        if (branchId != null && !Objects.equals(group.branchId(), branchId)) {
            errors.add(error("BRANCH_MISMATCH", "groupId", "Группа не относится к выбранному филиалу"));
        }
        if (group.status() != GroupStatus.ACTIVE) {
            errors.add(error("INVALID_STATUS", "groupId", "Группа должна быть активной"));
        }
        if (leadType != null && (group.audienceType() == null || !Objects.equals(group.audienceType().name(), leadType.name()))) {
            errors.add(error("AUDIENCE_MISMATCH", "groupId", "Тип группы не соответствует leadType"));
        }
        return group;
    }

    private CoachDto resolveCoach(UUID coachId, List<ContractValidationError> errors) {
        if (coachId == null) {
            return null;
        }
        try {
            return coachPort.getCoach(coachId);
        } catch (RuntimeException ex) {
            errors.add(error("NOT_FOUND", "coachId", "Тренер не найден"));
            return null;
        }
    }

    private void validateOverlap(UUID excludedContractId, UUID playerId, LocalDate startDate, LocalDate endDate, List<ContractValidationError> errors) {
        if (playerId == null || startDate == null) {
            return;
        }
        boolean overlap = endDate == null
                ? contractRepository.existsOpenEndedOverlap(playerId, startDate, excludedContractId)
                : contractRepository.existsOverlappingContractInRange(playerId, startDate, endDate, excludedContractId);
        if (overlap) {
            errors.add(error("OVERLAP", "participantId", "Для этого участника уже есть пересекающийся договор"));
        }
    }

    private void validateUpdatable(Contract contract) {
        if (!contract.getStatus().isEditable()) {
            throw new ContractValidationException(List.of(
                    error("IMMUTABLE_STATUS", "status", "Договор в статусе " + contract.getStatus() + " нельзя редактировать")
            ));
        }
    }

    private void validateExtendable(Contract contract) {
        if (!contract.getStatus().canBeExtended()) {
            throw new ContractValidationException(List.of(
                    error("IMMUTABLE_STATUS", "status", "Договор в статусе " + contract.getStatus() + " нельзя продлевать")
            ));
        }
    }

    private Contract findContract(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Contract not found", contractId));
    }

    private Player findPlayer(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found", playerId));
    }

    private Client requireParent(Player player) {
        if (player.getParent() == null) {
            throw new NotFoundException("Client not found for player", player.getId());
        }
        return player.getParent();
    }

    private boolean touchLifecycleStatus(Contract contract) {
        if (contract.getStatus() == ContractStatus.CANCELLED) {
            return false;
        }
        ContractStatus target = resolveLifecycleStatus(contract);
        if (target != contract.getStatus()) {
            contract.setStatus(target);
            return true;
        }
        return false;
    }

    private void touchLifecycleStatuses(Collection<Contract> contracts) {
        contracts.forEach(this::touchLifecycleStatus);
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

    private Map<UUID, Player> loadPlayers(Collection<Contract> contracts) {
        Set<UUID> playerIds = contracts.stream().map(Contract::getPlayerId).collect(Collectors.toSet());
        return playerRepository.findByIdIn(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, item -> item));
    }

    private Map<UUID, Client> loadClients(Collection<Player> players) {
        Set<UUID> clientIds = players.stream()
                .map(Player::getParent)
                .filter(Objects::nonNull)
                .map(Client::getId)
                .collect(Collectors.toSet());
        return clientRepository.findAllById(clientIds).stream()
                .collect(Collectors.toMap(Client::getId, item -> item));
    }

    private Map<UUID, GroupDto> loadGroups(Collection<Contract> contracts) {
        Set<UUID> groupIds = contracts.stream().map(Contract::getGroupId).collect(Collectors.toSet());
        return groupPort.getGroupsByIds(groupIds).stream()
                .collect(Collectors.toMap(GroupDto::groupId, item -> item));
    }

    private Map<UUID, CoachDto> loadCoaches(Collection<Contract> contracts) {
        Set<UUID> coachIds = contracts.stream()
                .map(Contract::getCoachId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (coachIds.isEmpty()) {
            return Map.of();
        }
        return coachPort.getCoaches(coachIds).stream()
                .collect(Collectors.toMap(CoachDto::id, item -> item));
    }

    private List<ContractHistoryOutput> loadHistory(UUID contractId) {
        return contractHistoryRepository.findByContractIdOrderByCreatedAtDesc(contractId).stream()
                .map(item -> new ContractHistoryOutput(
                        item.getId(),
                        item.getType(),
                        item.getCreatedAt(),
                        item.getActorName(),
                        item.getComment()
                ))
                .toList();
    }

    private List<StudentContractSnapshotOutput> toStudentContractSnapshots(List<Contract> contracts) {
        if (contracts.isEmpty()) {
            return List.of();
        }

        touchLifecycleStatuses(contracts);
        Map<UUID, Player> players = loadPlayers(contracts);
        Map<UUID, Client> clients = loadClients(players.values());
        Map<UUID, GroupDto> groups = loadGroups(contracts);
        Map<UUID, CoachDto> coaches = loadCoaches(contracts);

        return contracts.stream()
                .map(contract -> {
                    Player player = players.get(contract.getPlayerId());
                    Client client = player == null || player.getParent() == null ? null : clients.get(player.getParent().getId());
                    GroupDto group = groups.get(contract.getGroupId());
                    CoachDto coach = contract.getCoachId() == null ? null : coaches.get(contract.getCoachId());
                    return new StudentContractSnapshotOutput(
                            contract.getId(),
                            contract.getPlayerId(),
                            client == null ? null : client.getBranchId(),
                            contract.getContractNumber(),
                            contract.getStatus(),
                            contract.getStartDate(),
                            contract.getEndDate(),
                            defaultAmount(contract.getAmount()),
                            contract.getCurrency(),
                            contract.getGroupId(),
                            group == null ? null : group.name(),
                            contract.getCoachId(),
                            coach == null ? null : joinName(coach.firstName(), coach.lastName())
                    );
                })
                .toList();
    }

    private ContractListItemOutput toListItem(
            Contract contract,
            Player player,
            Map<UUID, Client> clients,
            Map<UUID, GroupDto> groups,
            Map<UUID, CoachDto> coaches
    ) {
        Client client = player == null || player.getParent() == null ? null : clients.get(player.getParent().getId());
        GroupDto group = groups.get(contract.getGroupId());
        CoachDto coach = contract.getCoachId() == null ? null : coaches.get(contract.getCoachId());
        return new ContractListItemOutput(
                contract.getId(),
                contract.getContractNumber(),
                client == null ? null : client.getBranchId(),
                contract.getLeadType(),
                contract.getStatus(),
                contract.getAmount(),
                contract.getCurrency(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getNotes(),
                player == null ? null : new ContractParticipantOutput(player.getId(), buildPlayerName(player), player.getBirthDate()),
                client == null ? null : toPrimaryContact(client),
                group == null ? null : toGroupOutput(group),
                toCoachOutput(coach),
                null,
                null,
                null,
                null,
                null,
                contract.getCreatedAt(),
                contract.getUpdatedAt()
        );
    }

    private ContractDetailsOutput toDetails(
            Contract contract,
            Player player,
            Client client,
            GroupDto group,
            CoachDto coach,
            List<ContractHistoryOutput> history
    ) {
        return new ContractDetailsOutput(
                contract.getId(),
                contract.getContractNumber(),
                client.getBranchId(),
                contract.getLeadType(),
                contract.getStatus(),
                contract.getAmount(),
                contract.getCurrency(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getNotes(),
                new ContractParticipantOutput(player.getId(), buildPlayerName(player), player.getBirthDate()),
                toPrimaryContact(client),
                toGroupOutput(group),
                toCoachOutput(coach),
                null,
                null,
                null,
                null,
                null,
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                history
        );
    }

    private ContractPrimaryContactOutput toPrimaryContact(Client client) {
        return new ContractPrimaryContactOutput(
                client.getId(),
                buildClientName(client),
                client.getPhone(),
                extractClientEmail(client)
        );
    }

    private ContractGroupOutput toGroupOutput(GroupDto group) {
        return new ContractGroupOutput(group.groupId(), group.name(), group.audienceType());
    }

    private ContractCoachOutput toCoachOutput(CoachDto coach) {
        if (coach == null) {
            return null;
        }
        return new ContractCoachOutput(coach.id(), buildCoachName(coach));
    }

    private void appendHistory(UUID contractId, ContractHistoryType type, UUID actorUserId, String requestedComment, String fallbackComment) {
        contractHistoryRepository.save(ContractHistory.builder()
                .contractId(contractId)
                .type(type)
                .actorUserId(actorUserId)
                .actorName(resolveActorName(actorUserId))
                .comment(trimToNull(requestedComment) == null ? fallbackComment : trimToNull(requestedComment))
                .build());
    }

    private String resolveActorName(UUID actorUserId) {
        return adminPort.findById(actorUserId)
                .map(this::buildAdminName)
                .filter(name -> !name.isBlank())
                .orElse(actorUserId == null ? "Unknown" : actorUserId.toString());
    }

    private String buildAdminName(AdminDto admin) {
        return joinName(admin.firstName(), admin.lastName());
    }

    private Client resolveOrCreateClient(UUID branchId, ContractPrimaryContactDraftInput draft) {
        String phone = trimToNull(draft.phone());
        Optional<Client> existing = phone == null ? Optional.empty() : clientRepository.findByPhone(phone);
        if (existing.isPresent()) {
            Client client = existing.get();
            if (client.getBranchId() == null) {
                client.setBranchId(branchId);
            }
            return client;
        }

        String normalizedEmail = resolveClientEmail(draft.email(), draft.phone());
        UUID userId = authPort.findUserIdByEmail(normalizedEmail)
                .orElseGet(() -> registerClientUser(normalizedEmail));

        String[] names = splitName(draft.fullName());
        return clientRepository.findById(userId)
                .orElseGet(() -> clientRepository.save(Client.builder()
                        .id(userId)
                        .firstName(names[0])
                        .lastName(names[1])
                        .phone(phone)
                        .branchId(branchId)
                        .comments(trimToNull(draft.email()))
                        .status(ClientStatus.ACTIVE)
                        .build()));
    }

    private Player resolveOrCreatePlayer(Client client, ContractParticipantDraftInput draft) {
        String[] names = splitName(draft.fullName());
        return playerRepository.findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(
                        client.getId(),
                        names[0],
                        names[1],
                        draft.birthDate()
                )
                .orElseGet(() -> playerRepository.save(Player.builder()
                        .id(UUID.randomUUID())
                        .firstName(names[0])
                        .lastName(names[1])
                        .birthDate(draft.birthDate())
                        .parent(client)
                        .build()));
    }

    private UUID registerClientUser(String email) {
        AuthRegisterCommandOutput output = authPort.register(AuthRegisterCommand.builder()
                .email(email)
                .password("Temp#" + UUID.randomUUID())
                .roles(Set.of(Role.CLIENT))
                .requireToChangePassword(true)
                .build());
        return output.id();
    }

    private LeadType resolveLeadType(UUID playerId, Map<UUID, Contract> latestContracts, Map<UUID, LeadType> leadTypes) {
        Contract contract = latestContracts.get(playerId);
        if (contract != null && contract.getLeadType() != null) {
            return contract.getLeadType();
        }
        return leadTypes.getOrDefault(playerId, LeadType.CHILDREN);
    }

    private String resolveContractNumber(String requestedNumber) {
        String normalized = trimToNull(requestedNumber);
        if (normalized != null) {
            return normalized;
        }

        String year = String.valueOf(LocalDate.now().getYear());
        long seed = contractRepository.count() + 1;
        String candidate = buildContractNumber(year, seed);
        while (contractRepository.existsByContractNumber(candidate)) {
            seed++;
            candidate = buildContractNumber(year, seed);
        }
        return candidate;
    }

    private String buildContractNumber(String year, long seed) {
        return "CNT-" + year + "-" + String.format(Locale.ROOT, "%05d", seed);
    }

    private String resolveCurrency(String currency) {
        String normalized = trimToNull(currency);
        return normalized == null ? DEFAULT_CURRENCY : normalized.toUpperCase(Locale.ROOT);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String buildPlayerName(Player player) {
        return joinName(player.getFirstName(), player.getLastName());
    }

    private String buildClientName(Client client) {
        return joinName(client.getFirstName(), client.getLastName());
    }

    private boolean belongsToBranch(Contract contract, UUID branchId) {
        Player player = findPlayer(contract.getPlayerId());
        return Objects.equals(requireParent(player).getBranchId(), branchId);
    }

    private String buildCoachName(CoachDto coach) {
        return joinName(coach.firstName(), coach.lastName());
    }

    private String joinName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }

    private String extractClientEmail(Client client) {
        String comments = trimToNull(client.getComments());
        return comments != null && comments.contains("@") ? comments : null;
    }

    private String resolveClientEmail(String email, String phone) {
        String normalizedEmail = trimToNull(email);
        if (normalizedEmail != null) {
            return normalizedEmail.toLowerCase(Locale.ROOT);
        }
        String normalizedPhone = phone == null ? UUID.randomUUID().toString().replace("-", "") : phone.replaceAll("[^0-9]", "");
        return "client+" + normalizedPhone + "@soccerhub.local";
    }

    private String[] splitName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (normalized.isEmpty()) {
            return new String[]{"Unknown", "Unknown"};
        }
        String[] parts = normalized.split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], parts[0]};
        }
        return parts;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSearchLike(String raw) {
        String normalized = trimToNull(raw);
        return normalized == null ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }

    private UUID parseUuid(String raw) {
        String normalized = trimToNull(raw);
        if (normalized == null) {
            return null;
        }
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ContractValidationError error(String code, String field, String message) {
        return new ContractValidationError(code, field, message);
    }

    private void throwIfErrors(List<ContractValidationError> errors) {
        if (errors != null && !errors.isEmpty()) {
            throw new ContractValidationException(errors);
        }
    }

    private record ResolvedParticipant(Player player, Client client) {
        private static ResolvedParticipant empty() {
            return new ResolvedParticipant(null, null);
        }
    }
}
