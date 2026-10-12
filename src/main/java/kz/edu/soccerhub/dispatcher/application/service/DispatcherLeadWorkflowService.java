package kz.edu.soccerhub.dispatcher.application.service;
import kz.edu.soccerhub.common.dto.lead.*;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor
public class DispatcherLeadWorkflowService {
 private final DispatcherBranchService branches;
 private final LeadPort leads;
 private final LeadWorkPort work;
 private final LeadRoutingPort routing;
 private final AdminPort admins;
 public UUID requireLead(UUID user,UUID id) { UUID branch=leads.getLeadBranchId(id);branches.requireOwnedBranch(user,branch);return branch; }
 public LeadOutput details(UUID user,UUID id) {requireLead(user,id);return leads.getLeadOutput(id,user);}
 public List<LeadActivityOutput> history(UUID user,UUID id) {requireLead(user,id);return leads.getLeadActivities(id);}
 public void validateAdmin(UUID user,UUID branch,UUID adminId) {
  if(adminId==null)return;
  var a=admins.findById(adminId).orElseThrow(()->new BadRequestException("Администратор не найден"));
  if(!a.isActive() || !user.equals(a.dispatcherId()) || a.branchesId()==null || !a.branchesId().contains(branch))throw new BadRequestException("Выберите активного администратора этого филиала");
 }
 @Transactional public void route(UUID user,UUID id,LeadRoutingCommand c) {requireLead(user,id);branches.requireActiveBranch(user,c.branchId());validateAdmin(user,c.branchId(),c.adminId());routing.route(id,user,c);}
 @Transactional public void update(UUID user,UUID id,LeadWorkCommand c) {requireLead(user,id);work.update(id,user,c);}
}
