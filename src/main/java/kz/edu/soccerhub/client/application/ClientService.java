package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.application.dto.ClientDto;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
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

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService implements ClientPort {

    private final ClientRepository clientRepository;
    private final BranchPort branchPort;

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

}
