package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.dto.client.AdminCreateClientStudentRelationInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminCreateClientStudentInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminEndClientStudentRelationInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminUpdateClientStudentRelationInput;
import kz.edu.soccerhub.admin.application.service.AdminClientStudentRelationService;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminClientStudentRelationControllerTest {

    private final AdminClientStudentRelationService service = Mockito.mock(AdminClientStudentRelationService.class);
    private AdminClientStudentRelationController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminClientStudentRelationController(service);
    }

    @Test
    void shouldForwardClientStudentCommands() {
        UUID adminId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());
        ClientStudentRelationOutput output = output(relationId, clientId, playerId);
        AdminCreateClientStudentRelationInput createInput = new AdminCreateClientStudentRelationInput(
                playerId, ClientStudentRelationshipType.MOTHER, true, true, true, true, LocalDate.now()
        );
        AdminCreateClientStudentInput createStudentInput = new AdminCreateClientStudentInput(
                "Student", "Name", LocalDate.of(2015, 1, 1), ClientStudentRelationshipType.MOTHER,
                true, true, true, true, LocalDate.now()
        );
        AdminUpdateClientStudentRelationInput updateInput = new AdminUpdateClientStudentRelationInput(
                ClientStudentRelationshipType.GUARDIAN, true, true, true, true
        );
        AdminEndClientStudentRelationInput endInput = new AdminEndClientStudentRelationInput(LocalDate.now());

        when(service.getClientStudents(adminId, clientId)).thenReturn(List.of(output));
        when(service.getStudentClients(adminId, playerId)).thenReturn(List.of(output));
        when(service.create(adminId, clientId, createInput)).thenReturn(output);
        when(service.createStudent(adminId, clientId, createStudentInput)).thenReturn(output);
        when(service.update(adminId, relationId, updateInput)).thenReturn(output);
        when(service.end(adminId, relationId, endInput)).thenReturn(output);

        assertSame(output, controller.getClientStudents(jwt, clientId).getBody().getFirst());
        assertSame(output, controller.getStudentClients(jwt, playerId).getBody().getFirst());
        assertSame(output, controller.create(jwt, clientId, createInput).getBody());
        assertSame(output, controller.createStudent(jwt, clientId, createStudentInput).getBody());
        assertSame(output, controller.update(jwt, relationId, updateInput).getBody());
        assertSame(output, controller.end(jwt, relationId, endInput).getBody());

        verify(service).getClientStudents(adminId, clientId);
        verify(service).getStudentClients(adminId, playerId);
        verify(service).create(adminId, clientId, createInput);
        verify(service).createStudent(adminId, clientId, createStudentInput);
        verify(service).update(adminId, relationId, updateInput);
        verify(service).end(adminId, relationId, endInput);
    }

    private ClientStudentRelationOutput output(UUID relationId, UUID clientId, UUID playerId) {
        return new ClientStudentRelationOutput(
                relationId,
                clientId,
                "Client Name",
                playerId,
                "Student Name",
                ClientStudentRelationshipType.MOTHER,
                true,
                true,
                true,
                true,
                LocalDate.now(),
                null,
                true
        );
    }
}
