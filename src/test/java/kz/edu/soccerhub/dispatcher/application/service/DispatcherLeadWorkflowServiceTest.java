package kz.edu.soccerhub.dispatcher.application.service;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.dto.lead.*;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class DispatcherLeadWorkflowServiceTest {
 final DispatcherBranchService branches=mock(DispatcherBranchService.class);
 final LeadPort leads=mock(LeadPort.class);
 final LeadWorkPort work=mock(LeadWorkPort.class);
 final LeadRoutingPort routing=mock(LeadRoutingPort.class);
 final AdminPort admins=mock(AdminPort.class);
 final DispatcherLeadWorkflowService service=new DispatcherLeadWorkflowService(branches,leads,work,routing,admins);
 final UUID user=UUID.randomUUID(),id=UUID.randomUUID(),branch=UUID.randomUUID(),target=UUID.randomUUID(),admin=UUID.randomUUID();
 @Test void foreignLeadCannotBeReadOrChanged() {
  when(leads.getLeadBranchId(id)).thenReturn(branch);
  doThrow(new BadRequestException("Нет доступа")).when(branches).requireOwnedBranch(user,branch);
  assertThrows(BadRequestException.class,()->service.details(user,id));
  assertThrows(BadRequestException.class,()->service.update(user,id,LeadWorkCommand.builder().build()));
  verify(leads,never()).getLeadOutput(any(),any());verifyNoInteractions(work,routing);
 }
 @Test void assigneeMustBeActiveOwnedAndAssignedToTarget() {
  for(var a:List.of(AdminDto.builder().id(admin).dispatcherId(UUID.randomUUID()).isActive(true).branchesId(Set.of(target)).build(),AdminDto.builder().id(admin).dispatcherId(user).isActive(false).branchesId(Set.of(target)).build(),AdminDto.builder().id(admin).dispatcherId(user).isActive(true).branchesId(Set.of(branch)).build())) {
   when(admins.findById(admin)).thenReturn(Optional.of(a));
   assertThrows(BadRequestException.class,()->service.validateAdmin(user,target,admin));
  }
 }
 @Test void transferValidatesBothBranchesBeforeOwnerMutation() {
  when(leads.getLeadBranchId(id)).thenReturn(branch);
  when(admins.findById(admin)).thenReturn(Optional.of(AdminDto.builder().id(admin).dispatcherId(user).isActive(true).branchesId(Set.of(target)).build()));
  var c=LeadRoutingCommand.builder().branchId(target).adminId(admin).version(3L).reason("Ближе к дому").build();service.route(user,id,c);
  var order=inOrder(branches,routing);order.verify(branches).requireOwnedBranch(user,branch);order.verify(branches).requireActiveBranch(user,target);order.verify(routing).route(id,user,c);
 }
}
