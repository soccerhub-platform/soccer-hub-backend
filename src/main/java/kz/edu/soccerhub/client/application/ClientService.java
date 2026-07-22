package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.application.dto.ClientDto;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ClientStudentRelationRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommandOutput;
import kz.edu.soccerhub.common.dto.client.ClientConversionCommand;
import kz.edu.soccerhub.common.dto.client.ClientConversionOutput;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.dto.student.StudentUpdateCommand;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.GroupMembershipPort;
import kz.edu.soccerhub.organization.application.service.GroupMembershipSyncService;
import kz.edu.soccerhub.organization.domain.model.GroupMembership;
import kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService implements ClientPort {

    private final ClientRepository clientRepository;
    private final PlayerRepository playerRepository;
    private final ContractRepository contractRepository;
    private final BranchPort branchPort;
    private final AuthPort authPort;
    private final GroupMembershipPort groupMembershipPort;
    private final GroupMembershipSyncService groupMembershipSyncService;
    private final ClientStudentRelationSyncService relationSyncService;
    private final ClientStudentRelationRepository relationRepository;

    @Override
    @Transactional
    public UUID createClient(String parentName, String phone, String email) {
        String[] names = splitName(parentName);

        Client client = Client.builder()
                .firstName(names[0])
                .lastName(names[1])
                .phone(phone)
                .email(email)
                .status(ClientStatus.NEW)
                .build();

        return clientRepository.save(client).getId();
    }

    @Override
    @Transactional
    public UUID createPlayer(UUID clientId, String childName, Integer childAge) {
        Client parent = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found", clientId));

        String[] names = splitName(childName);
        int ageYears = childAge == null ? 0 : Math.max(childAge, 0);

        Player player = Player.builder()
                .id(UUID.randomUUID())
                .firstName(names[0])
                .lastName(names[1])
                .birthDate(LocalDate.now().minusYears(ageYears))
                .parent(parent)
                .build();

        Player saved = playerRepository.save(player);
        relationSyncService.syncLegacyParent(saved);
        return saved.getId();
    }

    @Transactional
    public ClientCreateCommandOutput create(ClientCreateCommand command) {
        log.info("Creating client with command: {}", command);
        boolean isBranchExist = branchPort.isExist(command.branchId());
        if (!isBranchExist) {
            throw new NotFoundException("Branch not found", command.branchId());
        }

        Client client = Client.builder()
                .firstName(command.firstName())
                .lastName(command.lastName())
                .phone(command.phone())
                .branchId(command.branchId())
                .source(command.source())
                .comments(command.comments())
                .status(ClientStatus.NEW)
                .build();

        Client saved = clientRepository.save(client);
        return new ClientCreateCommandOutput(saved.getId());
    }

    public Collection<ClientDto> getAll() {
        log.info("Getting all clients");
        Collection<Client> clients = clientRepository.findAll();
        log.info("Found {} clients", clients.size());
        return clients.stream()
                .map(client -> ClientDto.builder()
                        .id(client.getId())
                        .name((client.getFirstName() + " " + client.getLastName()).trim())
                        .phone(client.getPhone())
                        .status(client.getStatus().name())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberDto> getGroupMembers(UUID groupId) {
        List<GroupMembership> memberships = groupMembershipPort.findActiveByGroupIdAsOfDate(groupId, LocalDate.now());
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<GroupMembership>> membershipsByPlayerId = memberships.stream()
                .collect(Collectors.groupingBy(GroupMembership::getPlayerId));
        Map<UUID, Player> playersById = playerRepository.findByIdIn(membershipsByPlayerId.keySet()).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        Map<UUID, List<Contract>> contractsByPlayerId = contractRepository.findByPlayerIdIn(membershipsByPlayerId.keySet()).stream()
                .collect(Collectors.groupingBy(Contract::getPlayerId));

        return membershipsByPlayerId.entrySet().stream()
                .map(entry -> toGroupMember(
                        entry.getValue(),
                        playersById.get(entry.getKey()),
                        contractsByPlayerId.getOrDefault(entry.getKey(), List.of())
                ))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(GroupMemberDto::childName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional
    public ClientConversionOutput convertLead(ClientConversionCommand command) {
        Client client = resolveOrCreateClient(command);
        Player player = resolveOrCreatePlayer(client, command.participantName(), command.participantBirthDate());
        Contract contract = resolveOrCreateContract(client.getId(), player.getId(), command);

        return new ClientConversionOutput(
                client.getId(),
                player.getId(),
                contract.getId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentProfileDto> getStudentProfilesByBranch(UUID branchId) {
        return playerRepository.findAllByParentBranchId(branchId).stream()
                .map(this::toStudentProfile)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileDto getStudentProfile(UUID playerId) {
        Player player = playerRepository.findWithParentById(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found", playerId));
        return toStudentProfile(player);
    }

    @Override
    @Transactional
    public StudentProfileDto updateStudent(UUID playerId, StudentUpdateCommand command) {
        Player player = playerRepository.findWithParentById(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found", playerId));

        player.setFirstName(command.firstName());
        player.setLastName(command.lastName());
        player.setBirthDate(command.birthDate());
        player.setPosition(command.position());

        return toStudentProfile(playerRepository.save(player));
    }

    @Override
    @Transactional(readOnly = true)
    public long countStudentsAsOf(UUID branchId, LocalDate date, String timezone) {
        ZoneId zoneId = resolveZone(timezone);
        return playerRepository.countByParentBranchIdAndCreatedAtBefore(
                branchId,
                date.plusDays(1).atStartOfDay(zoneId).toLocalDateTime()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCreatedStudents(UUID branchId, LocalDate date, String timezone) {
        ZoneId zoneId = resolveZone(timezone);
        return playerRepository.countByParentBranchIdAndCreatedAtBetween(
                branchId,
                date.atStartOfDay(zoneId).toLocalDateTime(),
                date.plusDays(1).atStartOfDay(zoneId).toLocalDateTime()
        );
    }

    private Client resolveOrCreateClient(ClientConversionCommand command) {
        if (command.existingClientId() != null) {
            return clientRepository.findById(command.existingClientId())
                    .orElseThrow(() -> new NotFoundException("Client from lead.clientId not found", command.existingClientId()));
        }

        String[] parentName = splitName(command.primaryContactName());
        String normalizedEmail = resolveClientEmail(command.email(), command.phone());
        UUID userId = authPort.findUserIdByEmail(normalizedEmail)
                .orElseGet(() -> registerClientUser(normalizedEmail));

        return clientRepository.findByUserId(userId)
                .orElseGet(() -> clientRepository.save(Client.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .firstName(parentName[0])
                        .lastName(parentName[1])
                        .phone(command.phone())
                        .email(command.email())
                        .branchId(command.branchId())
                        .source(command.source())
                        .comments(command.comments())
                        .status(ClientStatus.ACTIVE)
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

    private Player resolveOrCreatePlayer(Client client, String participantFullName, LocalDate birthDate) {
        String[] childName = splitName(participantFullName);

        Player player = playerRepository.findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(
                        client.getId(),
                        childName[0],
                        childName[1],
                        birthDate
                )
                .orElseGet(() -> playerRepository.save(Player.builder()
                        .id(UUID.randomUUID())
                        .firstName(childName[0])
                        .lastName(childName[1])
                        .birthDate(birthDate)
                        .parent(client)
                        .build()));
        relationSyncService.syncLegacyParent(player);
        return player;
    }

    private Contract resolveOrCreateContract(UUID clientId, UUID playerId, ClientConversionCommand command) {
        Contract contract = contractRepository.findFirstByPlayerIdAndGroupIdAndStartDateAndEndDate(
                        playerId,
                        command.groupId(),
                        command.contractStartDate(),
                        command.contractEndDate()
                )
                .orElseGet(() -> contractRepository.save(Contract.builder()
                        .id(UUID.randomUUID())
                        .playerId(playerId)
                        .clientId(clientId)
                        .groupId(command.groupId())
                        .contractNumber(generateContractNumber())
                        .leadType(command.leadType())
                        .status(ContractStatus.ACTIVE)
                        .coachId(null)
                        .startDate(command.contractStartDate())
                        .endDate(command.contractEndDate())
                        .amount(command.amount())
                        .currency("KZT")
                        .notes(command.comments())
                        .build()));

        if (contract.getClientId() == null) {
            contract.setClientId(clientId);
        }

        groupMembershipSyncService.syncFromContract(contract);
        return contract;
    }

    private String resolveClientEmail(String email, String phone) {
        String normalizedEmail = email == null ? null : email.trim();
        if (normalizedEmail != null && !normalizedEmail.isBlank()) {
            return normalizedEmail.toLowerCase();
        }
        String normalizedPhone = phone == null ? UUID.randomUUID().toString().replace("-", "") : phone.replaceAll("[^0-9]", "");
        return "client+" + normalizedPhone + "@soccerhub.local";
    }

    private String generateContractNumber() {
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
        return "CNT-" + year + "-" + String.format(java.util.Locale.ROOT, "%05d", seed);
    }

    private StudentProfileDto toStudentProfile(Player player) {
        Client client = resolvePrimaryClient(player);
        return new StudentProfileDto(
                client == null ? null : client.getBranchId(),
                player.getId(),
                joinName(player.getFirstName(), player.getLastName()),
                player.getFirstName(),
                player.getLastName(),
                player.getPosition(),
                player.getCreatedAt(),
                player.getBirthDate(),
                client == null ? null : client.getId(),
                client == null ? null : joinName(client.getFirstName(), client.getLastName()),
                client == null ? null : client.getPhone(),
                client == null ? null : trimToNull(client.getEmail()),
                client == null || client.getStatus() == null ? null : client.getStatus().name()
        );
    }

    private Client resolvePrimaryClient(Player player) {
        return relationRepository
                .findFirstByPlayerIdAndPrimaryContactTrueAndEndedAtIsNullOrderByStartedAtDesc(player.getId())
                .flatMap(relation -> clientRepository.findById(relation.getClientId()))
                .orElse(player.getParent());
    }

    private GroupMemberDto toGroupMember(
            List<GroupMembership> memberships,
            Player player,
            List<Contract> contracts
    ) {
        if (player == null) {
            return null;
        }

        List<GroupMembership> sortedMemberships = memberships.stream()
                .sorted(Comparator.comparing(GroupMembership::getJoinedAt))
                .toList();

        GroupMembership latestMembership = sortedMemberships.getLast();
        LocalDate joinedAt = sortedMemberships.getFirst().getJoinedAt();
        Contract currentContract = selectCurrentContract(contracts, latestMembership.getGroupId());
        Client primaryClient = resolvePrimaryClient(player);

        return new GroupMemberDto(
                latestMembership.getId(),
                primaryClient == null ? null : primaryClient.getId(),
                player.getId(),
                buildPlayerName(player),
                player.getBirthDate(),
                resolveMembershipStatus(latestMembership),
                currentContract == null || currentContract.getStatus() == null ? null : currentContract.getStatus().name(),
                currentContract == null ? null : currentContract.getId(),
                currentContract == null ? null : currentContract.getContractNumber(),
                currentContract == null ? null : currentContract.getStartDate(),
                currentContract == null ? null : currentContract.getEndDate(),
                joinedAt,
                latestMembership.getLeftAt()
        );
    }

    private Contract selectCurrentContract(List<Contract> contracts, UUID groupId) {
        if (contracts == null || contracts.isEmpty()) {
            return null;
        }

        LocalDate today = LocalDate.now();
        return contracts.stream()
                .filter(contract -> groupId.equals(contract.getGroupId()))
                .filter(contract -> contract.getStatus() != ContractStatus.CANCELLED)
                .sorted(Comparator
                        .comparing((Contract contract) -> isContractActiveOn(contract, today)).reversed()
                        .thenComparing(Contract::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Contract::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private boolean isContractActiveOn(Contract contract, LocalDate date) {
        return (contract.getStartDate() == null || !contract.getStartDate().isAfter(date))
                && (contract.getEndDate() == null || !contract.getEndDate().isBefore(date));
    }

    private String resolveMembershipStatus(GroupMembership membership) {
        GroupMembershipStatus status = membership.getStatus();
        if (status == null) {
            return GroupMembershipStatus.ACTIVE.name();
        }
        if (status == GroupMembershipStatus.UPCOMING
                && membership.getJoinedAt() != null
                && !membership.getJoinedAt().isAfter(LocalDate.now())) {
            return GroupMembershipStatus.ACTIVE.name();
        }
        if (status == GroupMembershipStatus.ACTIVE
                && membership.getJoinedAt() != null
                && membership.getJoinedAt().isAfter(LocalDate.now())) {
            return GroupMembershipStatus.UPCOMING.name();
        }
        return status.name();
    }

    private String buildPlayerName(Player player) {
        return joinName(player.getFirstName(), player.getLastName());
    }

    private String joinName(String firstName, String lastName) {
        String left = firstName == null ? "" : firstName.trim();
        String right = lastName == null ? "" : lastName.trim();
        return (left + " " + right).trim();
    }

    private ZoneId resolveZone(String timezone) {
        String value = timezone;
        if (value == null || value.isBlank()) {
            value = "Asia/Almaty";
        }
        return ZoneId.of(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

}
