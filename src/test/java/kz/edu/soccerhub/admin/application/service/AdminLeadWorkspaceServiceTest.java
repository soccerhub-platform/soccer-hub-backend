package kz.edu.soccerhub.admin.application.service;
import kz.edu.soccerhub.common.dto.lead.*;
import kz.edu.soccerhub.common.dto.trial.TrialBookingListItemDto;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AdminLeadWorkspaceServiceTest {
    LeadPort leads = mock(LeadPort.class);
    LeadWorkPort work = mock(LeadWorkPort.class);
    TrialPort trials = mock(TrialPort.class);
    AdminBranchAccessPort access = mock(AdminBranchAccessPort.class);
    AdminLeadWorkspaceService service = new AdminLeadWorkspaceService(leads, work, trials, access);
    UUID actor = UUID.randomUUID(), branch = UUID.randomUUID(), id = UUID.randomUUID();
    @Test void preferencesDeniedDoesNotMutate() {
        when(leads.getLeadBranchId(id)).thenReturn(branch);
        assertThrows(AccessDeniedException.class, () -> service.updatePreferences(actor, id, null));
        verifyNoInteractions(work);
    }
    @Test void branchDeniedDoesNotReadData() {
        assertThrows(AccessDeniedException.class, () -> service.get(actor, branch));
        verifyNoInteractions(leads, trials, work);
    }
    @Test void detailDeniedDoesNotReadTrials() {
        when(leads.getLeadBranchId(id)).thenReturn(branch);
        assertThrows(AccessDeniedException.class, () -> service.detail(actor, id));
        verifyNoInteractions(trials);
    }
    @Test void updateDeniedDoesNotMutate() {
        when(leads.getLeadBranchId(id)).thenReturn(branch);
        assertThrows(AccessDeniedException.class, () -> service.update(actor, id, null));
        verifyNoInteractions(work);
    }
    @Test void terminalStatusesAndEmptyColumnsRetained() {
        when(access.verifyAdminBelongsToBranch(actor, branch)).thenReturn(true);
        when(leads.getKanban(branch, actor)).thenReturn(Map.of(LeadStatus.NEW, List.of(),
                LeadStatus.CONVERTED, List.of(LeadOutput.builder().id(id).status(LeadStatus.CONVERTED).build())));
        when(trials.findByLeadIds(List.of(id))).thenReturn(List.of());
        assertEquals(1, service.get(actor, branch).columns().get(LeadStatus.CONVERTED).size());
    }
    @Test void usesActiveBookingPerParticipantNotLatestOtherChild() {
        UUID participant = UUID.randomUUID(), other = UUID.randomUUID();
        var historical = booking(participant, TrialBookingStatus.CANCELED);
        var active = booking(participant, TrialBookingStatus.SCHEDULED);
        var second = booking(other, TrialBookingStatus.COMPLETED);
        var result = AdminLeadWorkspaceService.enrich(LeadOutput.builder().id(id).build(), List.of(historical, active, second));
        assertEquals(List.of(active, second), result.currentTrials());
    }
    TrialBookingListItemDto booking(UUID participant, TrialBookingStatus status) {
        return TrialBookingListItemDto.builder().id(UUID.randomUUID()).leadId(id).participantId(participant).status(status).build();
    }
}
