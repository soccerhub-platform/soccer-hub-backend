package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.client.AdminCreateClientStudentRelationInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminCreateClientStudentInput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.ClientStudentRelationPort;
import kz.edu.soccerhub.common.port.ClientActivityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AdminClientStudentRelationServiceTest {

    @Mock private ClientStudentRelationPort relationPort;
    @Mock private AdminService adminService;
    @Mock private AdminBranchService adminBranchService;
    @Mock private ClientActivityPort clientActivityPort;

    private AdminClientStudentRelationService service;

    @BeforeEach
    void setUp() {
        service = new AdminClientStudentRelationService(relationPort, adminService, adminBranchService, clientActivityPort);
    }

    @Test
    void createShouldVerifyBranchesAndDelegateThroughPort() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        AdminCreateClientStudentRelationInput input = new AdminCreateClientStudentRelationInput(
                playerId, ClientStudentRelationshipType.MOTHER, true, true, false, false, true, true, LocalDate.now()
        );

        when(relationPort.getClientBranchId(clientId)).thenReturn(branchId);
        when(relationPort.getStudentBranchId(playerId)).thenReturn(branchId);
        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(relationPort.create(any())).thenReturn(new ClientStudentRelationOutput(
                UUID.randomUUID(), clientId, "Client", playerId, "Student",
                ClientStudentRelationshipType.MOTHER, true, true, true, true,
                input.startedAt(), null, true
        ));

        service.create(adminId, clientId, input);

        ArgumentCaptor<ClientStudentRelationCreateCommand> command = ArgumentCaptor.forClass(ClientStudentRelationCreateCommand.class);
        verify(relationPort).create(command.capture());
        assertEquals(clientId, command.getValue().clientId());
        assertEquals(playerId, command.getValue().playerId());
        verify(clientActivityPort).recordClientActivity(
                org.mockito.ArgumentMatchers.eq(clientId),
                org.mockito.ArgumentMatchers.eq(adminId),
                org.mockito.ArgumentMatchers.eq(ClientActivityType.STUDENT_LINKED),
                any()
        );
    }

    @Test
    void createShouldRejectCrossBranchRelationBeforeDelegation() {
        UUID adminId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(relationPort.getClientBranchId(clientId)).thenReturn(UUID.randomUUID());
        when(relationPort.getStudentBranchId(playerId)).thenReturn(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> service.create(
                adminId,
                clientId,
                new AdminCreateClientStudentRelationInput(
                        playerId, ClientStudentRelationshipType.FATHER, false, false, false, false, true, true, LocalDate.now()
                )
        ));

        verify(relationPort, never()).create(any());
    }

    @Test
    void createStudentShouldKeepClientContextAndRecordActivity() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        AdminCreateClientStudentInput input = new AdminCreateClientStudentInput(
                "Ayan", "Test", LocalDate.of(2015, 5, 10), ClientStudentRelationshipType.SELF,
                true, true, true, true, LocalDate.now()
        );
        ClientStudentRelationOutput output = new ClientStudentRelationOutput(
                UUID.randomUUID(), clientId, "Ayan Test", playerId, "Ayan Test",
                ClientStudentRelationshipType.SELF, true, true, true, true,
                input.startedAt(), null, true
        );
        when(relationPort.getClientBranchId(clientId)).thenReturn(branchId);
        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(relationPort.createStudent(any())).thenReturn(output);

        assertEquals(output, service.createStudent(adminId, clientId, input));

        ArgumentCaptor<ClientStudentCreateCommand> command = ArgumentCaptor.forClass(ClientStudentCreateCommand.class);
        verify(relationPort).createStudent(command.capture());
        assertEquals(clientId, command.getValue().clientId());
        assertEquals(ClientStudentRelationshipType.SELF, command.getValue().relationshipType());
        verify(clientActivityPort).recordClientActivity(
                org.mockito.ArgumentMatchers.eq(clientId), org.mockito.ArgumentMatchers.eq(adminId),
                org.mockito.ArgumentMatchers.eq(ClientActivityType.STUDENT_LINKED), any()
        );
    }
}
