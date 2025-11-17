package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.client.ClientCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommandOutput;

public interface ClientPort {
    ClientCreateCommandOutput create(ClientCreateCommand command);
}