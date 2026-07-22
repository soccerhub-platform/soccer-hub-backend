package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.client.ClientStudentRelationCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationEndCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationUpdateCommand;

import java.util.List;
import java.util.UUID;

public interface ClientStudentRelationPort {

    UUID getClientBranchId(UUID clientId);

    UUID getStudentBranchId(UUID playerId);

    UUID getRelationBranchId(UUID relationId);

    List<ClientStudentRelationOutput> getClientStudents(UUID clientId);

    List<ClientStudentRelationOutput> getStudentClients(UUID playerId);

    ClientStudentRelationOutput create(ClientStudentRelationCreateCommand command);

    ClientStudentRelationOutput createStudent(ClientStudentCreateCommand command);

    ClientStudentRelationOutput update(ClientStudentRelationUpdateCommand command);

    ClientStudentRelationOutput end(ClientStudentRelationEndCommand command);
}
