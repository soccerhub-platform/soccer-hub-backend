package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLeadServiceTest {

    @Mock
    private LeadPort leadPort;

    @Mock
    private AdminService adminService;

    @Mock
    private AdminBranchService adminBranchService;

    private AdminLeadService service;

    @BeforeEach
    void setUp() {
        service = new AdminLeadService(
                leadPort,
                adminService,
                adminBranchService
        );
    }

    @Test
    void rejectsDirectTrialLifecycleEvents() {
        UUID adminId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        when(adminService.findById(adminId))
                .thenReturn(Optional.of(
                        AdminDto.builder()
                                .id(adminId)
                                .build()
                ));
        when(leadPort.getLeadBranchId(leadId)).thenReturn(branchId);
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId))
                .thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> service.processEvent(
                        adminId,
                        leadId,
                        LeadEvent.CANCEL_TRIAL,
                        null,
                        null
                )
        );

        verify(leadPort, never()).processEvent(
                leadId,
                LeadEvent.CANCEL_TRIAL,
                null,
                null,
                adminId
        );
    }
}
