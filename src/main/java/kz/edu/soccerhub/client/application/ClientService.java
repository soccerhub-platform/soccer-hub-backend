package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.application.dto.ClientDto;
import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ClientStudentRelationRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import kz.edu.soccerhub.common.dto.client.*;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.dto.student.StudentUpdateCommand;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.organization.domain.model.GroupMembership;
import kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
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
    private final ClientStudentRelationSyncService relationSyncService;
    private final ClientStudentRelationRepository relationRepository;
    private final ClientStudentRelationPort clientStudentRelationPort;
    private final ClientActivityPort clientActivityPort;

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
                .sourceDetails(command.sourceDetails())
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
        ResolvedClient resolved = resolveOrCreateClient(command);
        Client client = resolved.client();

        if (resolved.created()) {
            Map<String, Object> createdPayload = new LinkedHashMap<>();
            createdPayload.put("source", "LEAD_CONVERSION");
            if (command.sourceLeadId() != null) {
                createdPayload.put("leadId", command.sourceLeadId());
            }

            clientActivityPort.recordClientActivity(
                    client.getId(),
                    command.actorUserId(),
                    ClientActivityType.CLIENT_CREATED,
                    "LEAD",
                    command.sourceLeadId(),
                    createdPayload
            );
        }

        String[] studentName = splitName(command.participantName());

        Player player = playerRepository.save(
                Player.builder()
                        .id(UUID.randomUUID())
                        .firstName(studentName[0])
                        .lastName(studentName[1])
                        .birthDate(command.participantBirthDate())
                        .parent(client)
                        .build()
        );

        ClientStudentRelationOutput relation =
                clientStudentRelationPort.create(
                        new ClientStudentRelationCreateCommand(
                                client.getId(),
                                player.getId(),
                                command.relationshipType(),
                                true,
                                true,
                                command.replacePrimaryContact(),
                                command.replacePrimaryPayer(),
                                command.relationshipType()
                                        == ClientStudentRelationshipType.SELF,
                                true,
                                LocalDate.now()
                        )
                );

        Map<String, Object> payload = new LinkedHashMap<>();
        if (command.sourceLeadId() != null) {
            payload.put("leadId", command.sourceLeadId());
        }
        payload.put("playerId", player.getId());
        payload.put("playerName", buildPlayerName(player));
        payload.put("relationId", relation.id());
        payload.put("relationshipType", command.relationshipType().name());

        clientActivityPort.recordClientActivity(
                client.getId(),
                command.actorUserId(),
                ClientActivityType.LEAD_CONVERTED,
                "LEAD",
                command.sourceLeadId(),
                payload
        );

        clientActivityPort.recordClientActivity(
                client.getId(),
                command.actorUserId(),
                ClientActivityType.STUDENT_LINKED,
                "LEAD",
                command.sourceLeadId(),
                payload
        );

        return new ClientConversionOutput(
                client.getId(),
                player.getId(),
                relation.id()
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

    private ResolvedClient resolveOrCreateClient(ClientConversionCommand command) {
        if (command.existingClientId() != null) {
            Client client = clientRepository.findById(command.existingClientId())
                    .orElseThrow(() -> new NotFoundException(
                            "Client from lead.clientId not found",
                            command.existingClientId()
                    ));

            return new ResolvedClient(client, false);
        }

        String[] parentName = splitName(command.primaryContactName());
        String normalizedEmail = resolveClientEmail(command.email(), command.phone());

        UUID userId = authPort.findUserIdByEmail(normalizedEmail)
                .orElseGet(() -> registerClientUser(normalizedEmail));

        Optional<Client> existingClient = clientRepository.findByUserId(userId);

        if (existingClient.isPresent()) {
            return new ResolvedClient(existingClient.get(), false);
        }

        Client createdClient = clientRepository.save(
                Client.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .firstName(parentName[0])
                        .lastName(parentName[1])
                        .phone(command.phone())
                        .email(command.email())
                        .branchId(command.branchId())
                        .source(command.source())
                        .sourceDetails(command.sourceDetails())
                        .comments(command.comments())
                        .status(ClientStatus.ACTIVE)
                        .build()
        );

        return new ResolvedClient(createdClient, true);
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


    private String resolveClientEmail(String email, String phone) {
        String normalizedEmail = email == null ? null : email.trim();
        if (normalizedEmail != null && !normalizedEmail.isBlank()) {
            return normalizedEmail.toLowerCase();
        }
        String normalizedPhone = phone == null ? UUID.randomUUID().toString().replace("-", "") : phone.replaceAll("[^0-9]", "");
        return "client+" + normalizedPhone + "@soccerhub.local";
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
                .orElse(null);
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
                .filter(contract -> contract.getStatus() != ContractStatus.CANCELLED).min(Comparator
                        .comparing((Contract contract) -> isContractActiveOn(contract, today)).reversed()
                        .thenComparing(Contract::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Contract::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
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

    private record ResolvedClient(
            Client client,
            boolean created
    ) {
    }
}
