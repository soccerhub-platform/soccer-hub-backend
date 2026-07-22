package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.client.ClientWorkspaceDetailsOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceListQuery;
import kz.edu.soccerhub.common.dto.client.ClientWorkspacePageOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceUpdateCommand;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceStatusCommand;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClientWorkspacePort {

    UUID getClientBranchId(UUID clientId);

    ClientWorkspacePageOutput getClients(UUID branchId, ClientWorkspaceListQuery query, Pageable pageable);

    ClientWorkspaceDetailsOutput getClient(UUID clientId);

    ClientWorkspaceDetailsOutput create(ClientWorkspaceCreateCommand command);

    ClientWorkspaceDetailsOutput update(ClientWorkspaceUpdateCommand command);

    ClientWorkspaceDetailsOutput changeStatus(ClientWorkspaceStatusCommand command);
}
