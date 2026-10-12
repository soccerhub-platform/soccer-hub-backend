package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.company.CompanyQuery;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DispatcherCompanyServiceTest {
    private final DispatcherClubService clubs = mock(DispatcherClubService.class);
    private final BranchPort branches = mock(BranchPort.class);
    private final CompanyClientReadPort clients = mock(CompanyClientReadPort.class);
    private final CompanyPaymentReadPort payments = mock(CompanyPaymentReadPort.class);
    private final CompanyLeadReadPort leads = mock(CompanyLeadReadPort.class);
    private final DispatcherCompanyService service = new DispatcherCompanyService(clubs, branches, clients, payments, leads);
    private final UUID user = UUID.randomUUID();
    private final UUID club = UUID.randomUUID();
    private final UUID branch = UUID.randomUUID();

    @BeforeEach
    void setupScope() {
        when(clubs.getAll(user)).thenReturn(List.of(club));
        when(branches.findByClubIds(List.of(club))).thenReturn(List.of(BranchDto.builder().id(branch).clubId(club).active(true).build()));
    }

    @Test
    void forwardsLeadAttentionWithoutRestrictingToTheDefaultMonth() {
        for (String attention : List.of("UNASSIGNED", "OVERDUE")) {
            service.page(user, "leads", branch, "", "", null, null, 0, 25, attention);
        }
        var query = ArgumentCaptor.forClass(CompanyQuery.class);
        verify(leads, times(2)).page(query.capture());
        assertEquals(List.of("UNASSIGNED", "OVERDUE"), query.getAllValues().stream().map(CompanyQuery::attention).toList());
        assertTrue(query.getAllValues().stream().allMatch(q -> q.branchIds().equals(Set.of(branch)) && !q.filterDates()));
    }

    @Test
    void forwardsEndingContractFilterToItsOwner() {
        service.page(user, "contracts", branch, "", "", null, null, 0, 25, "ENDING");
        var query = ArgumentCaptor.forClass(CompanyQuery.class);
        verify(clients).page(query.capture(), eq(true));
        assertEquals("ENDING", query.getValue().attention());
        verifyNoInteractions(leads, payments);
    }

    @Test
    void rejectsFiltersForTheWrongSection() {
        assertThrows(BadRequestException.class, () -> service.page(user, "payments", branch, "", "", null, null, 0, 25, "ENDING"));
        assertThrows(BadRequestException.class, () -> service.page(user, "leads", branch, "", "", null, null, 0, 25, "UNKNOWN"));
        verifyNoInteractions(clients, payments, leads);
    }

    @Test
    void attentionDoesNotBypassBranchOwnership() {
        assertThrows(BadRequestException.class, () -> service.page(user, "leads", UUID.randomUUID(), "", "", null, null, 0, 25, "OVERDUE"));
        verifyNoInteractions(clients, payments, leads);
    }

    @Test
    void keepsReceivedPaymentStatusAndSearchLimit() {
        service.page(user, "payments", branch, "", "PAID", null, null, 0, 25, "");
        var query = ArgumentCaptor.forClass(CompanyQuery.class);
        verify(payments).page(query.capture());
        assertEquals("PAID", query.getValue().status());
        assertThrows(BadRequestException.class, () -> service.page(user, "leads", branch, "x".repeat(201), "", null, null, 0, 25, ""));
        verifyNoInteractions(leads);
    }

    @Test void allTimeSummaryHasNoDateRestrictionAndPartialRangeIsRejected() {
        var summary=service.summary(user,branch,null,null);
        assertNull(summary.dateFrom());assertNull(summary.dateTo());
        var query=ArgumentCaptor.forClass(CompanyQuery.class);verify(leads).metrics(query.capture());
        assertFalse(query.getValue().filterDates());
        assertThrows(BadRequestException.class,()->service.summary(user,branch,java.time.LocalDate.now(),null));
    }
    @Test void sortIsForwardedToOwningReaderWithScope() {
        service.page(user,"leads",branch,"","",null,null,1,25,"","DUE");
        var query=ArgumentCaptor.forClass(CompanyQuery.class);verify(leads).page(query.capture());
        assertEquals("DUE",query.getValue().sort());assertEquals(1,query.getValue().page());assertEquals(Set.of(branch),query.getValue().branchIds());
    }
}
