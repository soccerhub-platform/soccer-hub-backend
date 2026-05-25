package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.application.dto.ClientDto;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommandOutput;
import kz.edu.soccerhub.common.dto.client.ClientConversionCommand;
import kz.edu.soccerhub.common.dto.client.ClientConversionOutput;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.ClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    @Override
    @Transactional
    public UUID createClient(String parentName, String phone, String email) {
        String[] names = splitName(parentName);

        Client client = Client.builder()
                .firstName(names[0])
                .lastName(names[1])
                .phone(phone)
                .comments(email)
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

        return playerRepository.save(player).getId();
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
        List<Contract> contracts = contractRepository.findByGroupId(groupId);
        if (contracts.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<Contract>> contractsByPlayerId = contracts.stream()
                .collect(Collectors.groupingBy(Contract::getPlayerId));
        Map<UUID, Player> playersById = playerRepository.findByIdIn(contractsByPlayerId.keySet()).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));

        return contractsByPlayerId.entrySet().stream()
                .map(entry -> toGroupMember(entry.getValue(), playersById.get(entry.getKey())))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(GroupMemberDto::childName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional
    public ClientConversionOutput convertLead(ClientConversionCommand command) {
        Client client = resolveOrCreateClient(command);
        Player player = resolveOrCreatePlayer(client, command.childName(), command.childBirthDate());
        Contract contract = resolveOrCreateContract(player.getId(), command);

        return new ClientConversionOutput(
                client.getId(),
                player.getId(),
                contract.getId()
        );
    }

    private Client resolveOrCreateClient(ClientConversionCommand command) {
        if (command.existingClientId() != null) {
            return clientRepository.findById(command.existingClientId())
                    .orElseThrow(() -> new NotFoundException("Client from lead.clientId not found", command.existingClientId()));
        }

        String[] parentName = splitName(command.parentName());
        String normalizedEmail = resolveClientEmail(command.email(), command.phone());
        UUID userId = authPort.findUserIdByEmail(normalizedEmail)
                .orElseGet(() -> registerClientUser(normalizedEmail));

        return clientRepository.findById(userId)
                .orElseGet(() -> clientRepository.save(Client.builder()
                        .id(userId)
                        .firstName(parentName[0])
                        .lastName(parentName[1])
                        .phone(command.phone())
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

    private Player resolveOrCreatePlayer(Client client, String childFullName, LocalDate birthDate) {
        String[] childName = splitName(childFullName);

        return playerRepository.findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(
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
    }

    private Contract resolveOrCreateContract(UUID playerId, ClientConversionCommand command) {
        return contractRepository.findFirstByPlayerIdAndGroupIdAndStartDateAndEndDate(
                        playerId,
                        command.groupId(),
                        command.contractStartDate(),
                        command.contractEndDate()
                )
                .orElseGet(() -> contractRepository.save(Contract.builder()
                        .id(UUID.randomUUID())
                        .playerId(playerId)
                        .groupId(command.groupId())
                        .startDate(command.contractStartDate())
                        .endDate(command.contractEndDate())
                        .amount(command.amount())
                        .build()));
    }

    private String resolveClientEmail(String email, String phone) {
        String normalizedEmail = email == null ? null : email.trim();
        if (normalizedEmail != null && !normalizedEmail.isBlank()) {
            return normalizedEmail.toLowerCase();
        }
        String normalizedPhone = phone == null ? UUID.randomUUID().toString().replace("-", "") : phone.replaceAll("[^0-9]", "");
        return "client+" + normalizedPhone + "@soccerhub.local";
    }

    private GroupMemberDto toGroupMember(List<Contract> contracts, Player player) {
        if (player == null) {
            return null;
        }

        List<Contract> sortedContracts = contracts.stream()
                .sorted(Comparator.comparing(Contract::getStartDate))
                .toList();

        Contract latestContract = sortedContracts.getLast();
        LocalDate joinedAt = sortedContracts.getFirst().getStartDate();

        return new GroupMemberDto(
                player.getParent() == null ? null : player.getParent().getId(),
                player.getId(),
                buildPlayerName(player),
                player.getBirthDate(),
                resolveContractStatus(latestContract),
                joinedAt
        );
    }

    private String resolveContractStatus(Contract contract) {
        LocalDate today = LocalDate.now();
        if (contract.getStartDate() != null && contract.getStartDate().isAfter(today)) {
            return "UPCOMING";
        }
        if (contract.getEndDate() == null || !contract.getEndDate().isBefore(today)) {
            return "ACTIVE";
        }
        return "EXPIRED";
    }

    private String buildPlayerName(Player player) {
        String firstName = player.getFirstName() == null ? "" : player.getFirstName().trim();
        String lastName = player.getLastName() == null ? "" : player.getLastName().trim();
        return (firstName + " " + lastName).trim();
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
