package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.enums.GroupMembershipStatus;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.common.port.GroupMembershipPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
@ExtendWith(MockitoExtension.class)
class GroupMembershipSyncServiceTest {

    @Mock
    private GroupMembershipPort groupMembershipPort;

    private GroupMembershipSyncService service;

    @BeforeEach
    void setUp() {
        service = new GroupMembershipSyncService(groupMembershipPort);
    }

    @Test
    void shouldResolveUpcomingMembershipForFutureContract() {
        Contract contract = baseContract();
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStartDate(LocalDate.now().plusDays(5));
        contract.setEndDate(LocalDate.now().plusDays(30));

        assertEquals(GroupMembershipStatus.UPCOMING, service.resolveStatus(contract));
        assertEquals(contract.getEndDate(), service.resolveLeftAt(contract));
    }

    @Test
    void shouldResolveCompletedMembershipForExpiredContract() {
        Contract contract = baseContract();
        contract.setStatus(ContractStatus.EXPIRED);
        contract.setStartDate(LocalDate.now().minusDays(30));
        contract.setEndDate(LocalDate.now().minusDays(1));

        assertEquals(GroupMembershipStatus.COMPLETED, service.resolveStatus(contract));
        assertEquals(contract.getEndDate(), service.resolveLeftAt(contract));
    }

    @Test
    void shouldResolveRemovedMembershipForCancelledContract() {
        Contract contract = baseContract();
        contract.setStatus(ContractStatus.CANCELLED);
        contract.setStartDate(LocalDate.now().minusDays(3));
        contract.setEndDate(LocalDate.now().plusDays(10));

        assertEquals(GroupMembershipStatus.REMOVED, service.resolveStatus(contract));
        assertEquals(LocalDate.now(), service.resolveLeftAt(contract));
    }

    private Contract baseContract() {
        return Contract.builder()
                .id(UUID.randomUUID())
                .groupId(UUID.randomUUID())
                .playerId(UUID.randomUUID())
                .build();
    }
}
