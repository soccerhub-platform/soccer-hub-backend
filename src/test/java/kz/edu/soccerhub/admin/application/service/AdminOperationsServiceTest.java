package kz.edu.soccerhub.admin.application.service;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class AdminOperationsServiceTest {
 @Mock AdminBranchAccessPort access; @Mock AdminPort admins; @Mock BranchPort branches;
 @Mock CompanyLeadOperationsPort leads; @Mock ContractFollowUpPort followUps; @Mock ContractPort contracts;
 @Mock CompanyOrganizationPort organization; @Mock CompanySessionReadPort sessions; @Mock CompanyTrialReadPort trials;
 @Mock CompanyClientReadPort clients; @Mock CompanyPaymentReadPort payments; @Mock CompanyLeadReadPort leadMetrics;
 @Mock CompanyPaymentHistoryPort paymentHistory; @Mock StudentJourneyPort journey;
 @InjectMocks AdminOperationsService service;
 UUID actor=UUID.randomUUID(),branch=UUID.randomUUID(),id=UUID.randomUUID();
 @Test void deniedBranchCannotReadQueuesOrReports() {
  assertThrows(ForbiddenException.class,()->service.tasks(actor,branch,"ALL","","",false,0,25));
  assertThrows(ForbiddenException.class,()->service.report(actor,branch,null,null));
  assertThrows(ForbiddenException.class,()->service.student(actor,branch,id));
  verifyNoInteractions(leads,clients,payments,trials,journey);
 }
 @Test void ownQueuePassesOwnerBeforePagination() {
  when(access.verifyAdminBelongsToBranch(actor,branch)).thenReturn(true);when(leads.tasks(any())).thenReturn(new OperationsPage(List.of(),0,0,2,25));
  service.tasks(actor,branch,"LEAD","","",true,2,25);
  verify(leads).tasks(argThat(q->q.branchIds().equals(Set.of(branch))&&actor.equals(q.ownerId())&&q.page()==2&&q.size()==25));
 }
 @Test void allTimeReportKeepsExactBranchAndNoComparison() {
  when(access.verifyAdminBelongsToBranch(actor,branch)).thenReturn(true);
  var report=service.report(actor,branch,null,null);assertNull(report.previous());
  verify(clients).metrics(argThat(q->q.branchIds().equals(Set.of(branch))&&!q.filterDates()));
  verify(payments).metrics(argThat(q->q.branchIds().equals(Set.of(branch))&&!q.filterDates()));
 }
 @Test void contractFromAnotherBranchCannotBeChanged() {
  when(contracts.getBranchId(id)).thenReturn(branch);
  assertThrows(ForbiddenException.class,()->service.updateFollowUp(actor,id,FollowUpCommand.builder().operation(FollowUpCommand.Operation.PLAN).version(0L).build()));
  verifyNoInteractions(followUps,admins);
 }
 @Test void unavailableAssigneeCannotReceiveFollowUp() {
  when(contracts.getBranchId(id)).thenReturn(branch);when(access.verifyAdminBelongsToBranch(actor,branch)).thenReturn(true);
  assertThrows(BadRequestException.class,()->service.updateFollowUp(actor,id,FollowUpCommand.builder().ownerId(UUID.randomUUID()).operation(FollowUpCommand.Operation.PLAN).version(0L).build()));verifyNoInteractions(followUps);
 }
}
