package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.client.PlayerCreateCommand;
import kz.edu.soccerhub.common.dto.client.PlayerCreateCommandOutput;

public interface PlayerPort {
    PlayerCreateCommandOutput create(PlayerCreateCommand command);
}