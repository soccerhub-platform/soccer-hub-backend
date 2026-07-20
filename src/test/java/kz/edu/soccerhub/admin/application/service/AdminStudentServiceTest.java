package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.student.AdminStudentDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentUpdateInput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.dto.student.StudentUpdateCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.ClientPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminStudentServiceTest {

    @Mock private ClientPort clientPort;
    @Mock private AdminService adminService;
    @Mock private AdminBranchService adminBranchService;
    @Mock private AdminStudentReadService adminStudentReadService;

    private AdminStudentService service;

    @BeforeEach
    void setUp() {
        service = new AdminStudentService(clientPort, adminService, adminBranchService, adminStudentReadService);
    }

    @Test
    void updatesStudentAndReturnsFreshDetails() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        LocalDate birthDate = LocalDate.of(2015, 6, 1);
        AdminStudentUpdateInput input = new AdminStudentUpdateInput(" UX ", " Child ", birthDate, " Forward ");
        AdminStudentDetailsOutput details = new AdminStudentDetailsOutput(
                null, null, null, null, null, List.of(), List.of(), List.of(), null
        );

        when(clientPort.getStudentProfile(playerId)).thenReturn(profile(branchId, playerId));
        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(adminStudentReadService.getStudent(adminId, playerId)).thenReturn(details);

        assertEquals(details, service.update(adminId, playerId, input));

        ArgumentCaptor<StudentUpdateCommand> command = ArgumentCaptor.forClass(StudentUpdateCommand.class);
        verify(clientPort).updateStudent(eq(playerId), command.capture());
        assertEquals("UX", command.getValue().firstName());
        assertEquals("Child", command.getValue().lastName());
        assertEquals(birthDate, command.getValue().birthDate());
        assertEquals("Forward", command.getValue().position());
    }

    @Test
    void rejectsStudentFromUnavailableBranch() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        when(clientPort.getStudentProfile(playerId)).thenReturn(profile(branchId, playerId));
        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.update(
                adminId,
                playerId,
                new AdminStudentUpdateInput("UX", "Child", LocalDate.of(2015, 6, 1), null)
        ));

        verify(clientPort, never()).updateStudent(any(), any());
        verifyNoInteractions(adminStudentReadService);
    }

    private StudentProfileDto profile(UUID branchId, UUID playerId) {
        return new StudentProfileDto(
                branchId,
                playerId,
                "UX Child",
                "UX",
                "Child",
                null,
                null,
                LocalDate.of(2015, 6, 1),
                UUID.randomUUID(),
                "Parent",
                "+77010000000",
                "parent@example.com",
                "ACTIVE"
        );
    }
}
