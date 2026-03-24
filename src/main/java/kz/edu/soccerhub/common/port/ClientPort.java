package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.client.ClientCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommandOutput;

import java.util.UUID;

public interface ClientPort {

    UUID createClient(
            String parentName,
            String phone,
            String email
    );

    UUID createPlayer(
            UUID clientId,
            String childName,
            Integer childAge
    );

    ClientCreateCommandOutput create(ClientCreateCommand command);
}