package kz.edu.soccerhub.crm.application.service;
import kz.edu.soccerhub.common.dto.lead.LeadRoutingCommand;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor
public class LeadRoutingService implements LeadRoutingPort {
 private final LeadRepository repository;
 private final LeadActivityService activities;
 private final TrialPort trials;
 private final BranchPort branches;
 private final AdminPort admins;
 @Override @Transactional public void route(UUID id,UUID actor,LeadRoutingCommand c) {
  var lead=repository.findLockedById(id).orElseThrow(()->new NotFoundException("Лид не найден",id));
  if(c.version()==null || c.version()!=lead.getWorkVersion()) throw new ConflictException("Лид уже изменён. Обновите карточку.","LEAD_WORK_CONFLICT",Map.of("leadId",id));
  if(lead.getStatus()==LeadStatus.LOST || lead.getStatus()==LeadStatus.CONVERTED) throw new BadRequestException("Закрытый лид нельзя передать");
  if(c.reason()==null || c.reason().isBlank()) throw new BadRequestException("Укажите причину передачи");
  UUID previousBranch=lead.getBranchId(),previousAdmin=lead.getAssignedAdminId();
  if(!previousBranch.equals(c.branchId()) && (lead.getClientId()!=null || !trials.findByLeadIds(List.of(id)).isEmpty())) throw new BadRequestException("Лид с пробными или клиентом нельзя переносить в другой филиал. Сначала завершите связанный процесс.");
  lead.route(c.branchId(),c.adminId());
  lead.updateWork(lead.getWorkPriority(),lead.getNextAction(),lead.getNextActionAt(),false);
  activities.logWork(lead,actor,"Распределение · филиал: "+branchName(previousBranch)+" → "+branchName(c.branchId())+" · ответственный: "+adminName(previousAdmin)+" → "+adminName(c.adminId())+"\nПричина: "+c.reason().trim());
 }
 private String branchName(UUID id) {return branches.findById(id).map(b->b.name()).orElse("Филиал недоступен");}
 private String adminName(UUID id) {return id==null?"Не назначен":admins.findById(id).map(a->a.firstName()+" "+a.lastName()).orElse("Администратор недоступен");}
}
