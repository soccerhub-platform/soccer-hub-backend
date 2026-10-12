package kz.edu.soccerhub.trial.application.service;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import kz.edu.soccerhub.trial.domain.repository.TrialBookingRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.time.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class CompanyTrialReader implements CompanyTrialReadPort {
 private final TrialBookingRepository repository;
 private final TrialBookingListReader reader;
 private final TrialSessionPort sessions;
 private final GroupPort groups;
 private final LeadPort leads;
 private final CompanyLeadOperationsPort leadOperations;
 private Specification<TrialBooking> filter(Set<UUID> ids,String status,boolean tasks,CompanyQuery q) {
  var assigned=q.ownerId()==null?null:leadOperations.assignedLeadIds(q.ownerId(),q.branchIds());
  return (r,c,b)-> {
   var scoped=r.get("trainingSessionId").in(ids);
   if(tasks) {
    if(assigned!=null) scoped=b.and(scoped,assigned.isEmpty()?b.disjunction():r.get("leadId").in(assigned));
    var action=b.or(b.isNotNull(r.get("nextActionAt")),b.and(b.equal(r.get("attendanceStatus").as(String.class),"ATTENDED"),b.equal(r.get("result").as(String.class),"PENDING"),b.isNotNull(r.get("coachRecommendation"))));
    if(status.equals("OVERDUE")) action=b.and(action,b.lessThan(r.get("nextActionAt"),LocalDateTime.now(ZoneId.of("Asia/Almaty"))));
    if(status.equals("TODAY")) action=b.and(action,b.greaterThanOrEqualTo(r.get("nextActionAt"),q.dateFrom().atStartOfDay()),b.lessThan(r.get("nextActionAt"),q.dateTo().plusDays(1).atStartOfDay()));
    return b.and(scoped,action,b.notEqual(r.get("status").as(String.class),"CANCELED"));
   }
   return switch(status) {
    case "ATTENDED","NO_SHOW","UNMARKED" -> b.and(scoped,b.equal(r.get("attendanceStatus").as(String.class),status));
    case "FOLLOW_UP","INTERESTED","NOT_INTERESTED","CONVERTED","PENDING" -> b.and(scoped,b.equal(r.get("result").as(String.class),status));
    case "" -> scoped;
    default -> b.and(scoped,b.equal(r.get("status").as(String.class),status));
   };
  };
 }
 public Map<String,Long> summary(Set<UUID> ids,CompanyQuery q) {
  if(ids.isEmpty())return Map.of();
  var counts=new LinkedHashMap<String,Long>();
  for(String s:List.of("SCHEDULED","ATTENDED","NO_SHOW","UNMARKED","FOLLOW_UP","PENDING","CONVERTED"))counts.put(s,repository.count(filter(ids,s,false,q)));
  counts.put("AWAITING_RESULT",repository.count(filter(ids,"ATTENDED",false,q).and((r,c,b)->b.equal(r.get("result").as(String.class),"PENDING"))));
  return counts;
 }
 public TrialCompanyPage page(Set<UUID> ids,CompanyQuery q) {
  if(ids.isEmpty())return new TrialCompanyPage(List.of(),0,0,q.page(),q.size(),Map.of());
  var spec=filter(ids,q.status(),false,q);
  var page=reader.read(repository.findAll(spec,PageRequest.of(q.page(),q.size(),Sort.by("OLDEST".equals(q.sort())?Sort.Direction.ASC:Sort.Direction.DESC,"createdAt").and(Sort.by("id")))));
  var counts=summary(ids,q);
  return new TrialCompanyPage(page.getContent(),page.getTotalElements(),page.getTotalPages(),q.page(),q.size(),counts);
 }
 public OperationsPage tasks(Set<UUID> ids,CompanyQuery q) {
  if(ids.isEmpty())return new OperationsPage(List.of(),0,0,q.page(),q.size());
  var page=reader.read(repository.findAll(filter(ids,q.status(),true,q),PageRequest.of(q.page(),q.size(),Sort.by("nextActionAt").and(Sort.by("id")))));
  var rows=page.getContent().stream().map(t->{var branch=groups.getGroupById(sessions.getSessionDetails(t.trainingSessionId()).groupId()).branchId();var lead=t.leadId()==null?null:leads.getLeadOutput(t.leadId(),null);return OperationsRow.builder().id(t.id()).branchId(branch).kind("TRIAL").name(t.studentName()).contact(t.leadName()).phone(t.leadPhone()).status(t.result().name()).ownerId(lead==null||lead.assignedAdmin()==null?null:lead.assignedAdmin().id()).nextAction(t.nextActionAt()==null?"REVIEW_RECOMMENDATION":t.nextActionType()==null?"OTHER":t.nextActionType().name()).dueAt(t.nextActionAt()==null?null:t.nextActionAt().atZone(ZoneId.of("Asia/Almaty")).toInstant()).startsAt(t.sessionStartsAt()).priority("NORMAL").coachName(t.coachName()).build();}).toList();
  return new OperationsPage(rows,page.getTotalElements(),page.getTotalPages(),q.page(),q.size());
 }
}
