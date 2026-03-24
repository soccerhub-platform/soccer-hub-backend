package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.application.dto.ClientDto;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommandOutput;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.ClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService implements ClientPort {

    private final ClientRepository clientRepository;
    private final PlayerRepository playerRepository;
    private final BranchPort branchPort;

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
