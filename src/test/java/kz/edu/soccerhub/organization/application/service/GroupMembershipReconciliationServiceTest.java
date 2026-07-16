package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.organization.domain.repository.GroupMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMembershipReconciliationServiceTest {

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Test
    void shouldCompleteExpiredMembershipsBeforeActivatingStartedOnes() {
        LocalDate today = LocalDate.of(2026, 7, 16);
        when(groupMembershipRepository.completeExpiredMemberships(eq(today), any(LocalDateTime.class), any(String.class)))
                .thenReturn(2);
        when(groupMembershipRepository.activateStartedMemberships(eq(today), any(LocalDateTime.class), any(String.class)))
                .thenReturn(3);

        GroupMembershipReconciliationService service = new GroupMembershipReconciliationService(groupMembershipRepository);

        GroupMembershipReconciliationService.Result result = service.reconcile(today);

        assertEquals(3, result.activated());
        assertEquals(2, result.completed());
        InOrder order = inOrder(groupMembershipRepository);
        order.verify(groupMembershipRepository).completeExpiredMemberships(eq(today), any(LocalDateTime.class), any(String.class));
        order.verify(groupMembershipRepository).activateStartedMemberships(eq(today), any(LocalDateTime.class), any(String.class));
    }
}
