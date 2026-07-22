package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.client.AdminCreateClientStudentRelationInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminCreateClientStudentInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminEndClientStudentRelationInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminUpdateClientStudentRelationInput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationEndCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationUpdateCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientStudentRelationPort;
import kz.edu.soccerhub.common.port.ClientActivityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminClientStudentRelationService {

    private final ClientStudentRelationPort relationPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final ClientActivityPort clientActivityPort;

    @Transactional(readOnly = true)
    public List<ClientStudentRelationOutput> getClientStudents(UUID adminId, UUID clientId) {
        verifyAdminAccess(adminId, relationPort.getClientBranchId(clientId));
        return relationPort.getClientStudents(clientId);
    }

    @Transactional(readOnly = true)
    public List<ClientStudentRelationOutput> getStudentClients(UUID adminId, UUID playerId) {
        verifyAdminAccess(adminId, relationPort.getStudentBranchId(playerId));
        return relationPort.getStudentClients(playerId);
    }

    @Transactional
    public ClientStudentRelationOutput create(UUID adminId, UUID clientId, AdminCreateClientStudentRelationInput input) {
        UUID clientBranchId = relationPort.getClientBranchId(clientId);
        UUID studentBranchId = relationPort.getStudentBranchId(input.playerId());
        if (!Objects.equals(clientBranchId, studentBranchId)) {
            throw new BadRequestException("Client and student belong to different branches", clientId, input.playerId());
        }
        verifyAdminAccess(adminId, clientBranchId);
        ClientStudentRelationOutput output = relationPort.create(new ClientStudentRelationCreateCommand(
                clientId, input.playerId(), input.relationshipType(), input.primaryContact(), input.primaryPayer(),
                input.legalRepresentative(), input.receivesNotifications(), input.startedAt()
        ));
        clientActivityPort.recordClientActivity(clientId, adminId, ClientActivityType.STUDENT_LINKED,
                Map.of("playerId", output.playerId(), "playerName", output.playerName(), "relationshipType", output.relationshipType().name()));
        return output;
    }

    @Transactional
    public ClientStudentRelationOutput createStudent(UUID adminId, UUID clientId, AdminCreateClientStudentInput input) {
        UUID clientBranchId = relationPort.getClientBranchId(clientId);
        verifyAdminAccess(adminId, clientBranchId);
        ClientStudentRelationOutput output = relationPort.createStudent(new ClientStudentCreateCommand(
                clientId, input.firstName(), input.lastName(), input.birthDate(), input.relationshipType(),
                input.primaryContact(), input.primaryPayer(), input.legalRepresentative(),
                input.receivesNotifications(), input.startedAt()
        ));
        clientActivityPort.recordClientActivity(clientId, adminId, ClientActivityType.STUDENT_LINKED,
                Map.of("playerId", output.playerId(), "playerName", output.playerName(),
                        "relationshipType", output.relationshipType().name()));
        return output;
    }

    @Transactional
    public ClientStudentRelationOutput update(UUID adminId, UUID relationId, AdminUpdateClientStudentRelationInput input) {
        verifyAdminAccess(adminId, relationPort.getRelationBranchId(relationId));
        ClientStudentRelationOutput output = relationPort.update(new ClientStudentRelationUpdateCommand(
                relationId, input.relationshipType(), input.primaryContact(), input.primaryPayer(),
                input.legalRepresentative(), input.receivesNotifications()
        ));
        clientActivityPort.recordClientActivity(output.clientId(), adminId, ClientActivityType.STUDENT_RELATION_UPDATED,
                Map.of("playerId", output.playerId(), "playerName", output.playerName(), "relationshipType", output.relationshipType().name()));
        return output;
    }

    @Transactional
    public ClientStudentRelationOutput end(UUID adminId, UUID relationId, AdminEndClientStudentRelationInput input) {
        verifyAdminAccess(adminId, relationPort.getRelationBranchId(relationId));
        ClientStudentRelationOutput output = relationPort.end(new ClientStudentRelationEndCommand(relationId, input.endedAt()));
        clientActivityPort.recordClientActivity(output.clientId(), adminId, ClientActivityType.STUDENT_RELATION_ENDED,
                Map.of("playerId", output.playerId(), "playerName", output.playerName(), "endedAt", input.endedAt().toString()));
        return output;
    }

    private void verifyAdminAccess(UUID adminId, UUID branchId) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        if (branchId == null || !adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }
}
