package kz.edu.soccerhub.dispatcher.application.service;
import kz.edu.soccerhub.common.dto.coach.*;
import kz.edu.soccerhub.common.dto.auth.*;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.dispatcher.application.dto.coach.*;
import kz.edu.soccerhub.dispatcher.application.dto.admin.DispatcherEmployeePermissionsInput;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class DispatcherCoachService {
 private final CoachPort coachPort;
 private final AuthPort auth;
 private final AdminPort admins;
 private final EmployeeAccessPort access;
 private final PasswordGenerator passwords;
 private final DispatcherBranchService branches;
 private Set<UUID> ownedBranches(UUID user) { return branches.getDispatcherBranches(user).stream().map(b->b.branchId()).collect(Collectors.toSet()); }
 private Set<String> creators(UUID user) { var result=admins.findAllByDispatcherId(user).stream().map(a->a.id().toString()).collect(Collectors.toSet());result.add(user.toString());return result; }
 private boolean manageable(UUID user,CoachDto coach,Set<UUID> ids) {
  var owned=ownedBranches(user);
  return owned.containsAll(ids) && (!ids.isEmpty() || creators(user).contains(coach.createdBy()));
 }
 private CoachDto requireOwned(UUID user,UUID coachId) {
  var coach=coachPort.getCoach(coachId);
  if(!manageable(user,coach,coachPort.getBranchIds(coachId))) throw new BadRequestException("Нет права менять сотрудника другой компании",coachId);
  return coach;
 }
 @Transactional(readOnly=true)
 public Page<CoachDto> getCoaches(UUID user,UUID branch,Pageable pageable) {
  branches.requireOwnedBranch(user,branch);return coachPort.getCoaches(Set.of(branch),pageable);
 }
 @Transactional(readOnly=true)
 public Page<DispatcherCoachOutput> list(UUID user,UUID branch,String search,int page,int size) {
  if(page<0 || page>100000 || size<1 || size>100 || search.length()>200) throw new BadRequestException("Некорректные параметры списка");
  var owned=ownedBranches(user);if(branch!=null) branches.requireOwnedBranch(user,branch);
  var pageable=PageRequest.of(page,size,Sort.by("firstName","lastName","id"));
  var profiles=branch!=null?coachPort.getCompanyCoaches(Set.of(branch),Set.of("__none__"),search,pageable):coachPort.getCompanyCoaches(owned.isEmpty()?Set.of(new UUID(0,0)):owned,creators(user),search,pageable);
  return profiles.map(c->{var ids=coachPort.getBranchIds(c.id());return new DispatcherCoachOutput(c,ids,manageable(user,c,ids));});
 }
 @Transactional
 public Map<String,Object> create(UUID user,DispatcherCoachInput input) {
  if(input.branchId()==null) throw new BadRequestException("Выберите филиал");
  branches.requireActiveBranch(user,input.branchId());
  String password=passwords.generate(12);
  var account=auth.register(AuthRegisterCommand.builder().email(input.email()).password(password).roles(Set.of(Role.COACH)).requireToChangePassword(true).build());
  coachPort.create(CoachCreateCommand.builder().id(account.id()).firstName(input.firstName().trim()).lastName(input.lastName().trim()).birthDate(input.birthDate()).email(account.email()).phone(input.phone()).bio(input.bio()).build());
  coachPort.assignToBranch(account.id(),input.branchId());
  if(input.specialization()!=null) coachPort.update(CoachUpdateCommand.builder().coachId(account.id()).firstName(input.firstName()).lastName(input.lastName()).email(account.email()).phone(input.phone()).birthDate(input.birthDate()).specialization(input.specialization()).bio(input.bio()).build());
  return Map.of("id",account.id(),"tempPassword",password);
 }
 @Transactional
 public void update(UUID user,UUID id,DispatcherCoachInput input) {
  requireOwned(user,id);auth.updateEmail(id,input.email());
  coachPort.update(CoachUpdateCommand.builder().coachId(id).firstName(input.firstName()).lastName(input.lastName()).email(input.email()).birthDate(input.birthDate()).phone(input.phone()).specialization(input.specialization()).bio(input.bio()).build());
 }
 @Transactional
 public void status(UUID user,UUID id,boolean enabled) {
  var before=requireOwned(user,id);
  if(enabled) {coachPort.enableCoach(id);auth.enableUser(id);} else {coachPort.disableCoach(id);auth.disableUser(id);}
  coachPort.recordStatusHistory(id,enabled?kz.edu.soccerhub.coach.domain.model.enums.CoachStatus.ACTIVE:kz.edu.soccerhub.coach.domain.model.enums.CoachStatus.INACTIVE,user);
 }
 @Transactional
 public void assignment(UUID user,UUID id,UUID branch,boolean assign) {
  requireOwned(user,id);branches.requireOwnedBranch(user,branch);
  if(assign) {branches.requireActiveBranch(user,branch);coachPort.assignToBranch(id,branch);} else coachPort.unassignFromBranch(id,branch);
 }
 public EmployeeAccessDto permissions(UUID user,UUID id) {requireOwned(user,id);return access.getAccess(id);}
 @Transactional
 public void permissions(UUID user,UUID id,DispatcherEmployeePermissionsInput input) {
  requireOwned(user,id);access.changePermissions(new ChangeEmployeePermissionsCommand(id,user,input.permissions()));
 }
 @Transactional
 public Map<String,String> resetPassword(UUID user,UUID id) {requireOwned(user,id);String password=passwords.generate(12);auth.resetPassword(id,password);return Map.of("tempPassword",password);}
}
