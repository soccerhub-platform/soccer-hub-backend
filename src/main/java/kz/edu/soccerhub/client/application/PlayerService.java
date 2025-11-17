package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.dto.client.PlayerCreateCommand;
import kz.edu.soccerhub.common.dto.client.PlayerCreateCommandOutput;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.PlayerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService implements PlayerPort {

    private final ClientRepository clientRepository;
    private final PlayerRepository playerRepository;

    @Override
    public PlayerCreateCommandOutput create(PlayerCreateCommand command) {
        log.info("Creating player with command: {}", command);
        Client parent = clientRepository.findById(command.parentId())
                .orElseThrow(() -> new NotFoundException("Client not found", command.parentId()));

        Player player = Player.builder()
                .firstName(command.firstName())
                .lastName(command.lastName())
                .parent(parent)
                .build();

        Player saved = playerRepository.save(player);
        return new PlayerCreateCommandOutput(saved.getId());
    }
}