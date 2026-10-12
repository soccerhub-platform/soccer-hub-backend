package kz.edu.soccerhub.dispatcher.application.service;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import java.time.*;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class DispatcherOperationsService {
 private final DispatcherCompanyService company;
 private final DispatcherBranchService branches;
 private final DispatcherLeadWorkflowService leadWorkflow;
 private final CompanyLeadOperationsPort leads;
 private final ContractFollowUpPort followUp;
 private final ContractPort contracts;
 private final AdminPort admins;
 private final CompanyOrganizationPort organization;
 private final CompanySessionReadPort sessionReader;
 private final TrialSessionPort sessions;
 private final GroupPort groups;
 private final CoachPort coaches;
 private final TrialPort trials;
 private final TrialWorkflowPort trialWorkflow;
 private final CompanyTrialReadPort trialReader;
 private final CompanyStudentHistoryPort students;
 private final CompanyPaymentHistoryPort payments;
 private final LeadPort leadPort;
 public Collection<AdminDto> assignees(UUID user,UUID branch) {branches.requireOwnedBranch(user,branch);return admins.findAllByBranchId(branch).stream().filter(a->a.isActive()&&user.equals(a.dispatcherId())).toList();}
 public CompanyEntry contractRecord(UUID user,UUID id) {
  branches.requireOwnedBranch(user,contracts.getBranchId(id));var c=contracts.getById(id);
  return CompanyEntry.builder().id(id).branchId(c.branchId()).reference(c.contractNumber()).name(c.participant().fullName()).contact(c.primaryContact().fullName()).phone(c.primaryContact().phone()).email(c.primaryContact().email()).birthDate(c.participant().birthDate()).status(c.status().name()).amount(c.amount()).currency(c.currency()).startDate(c.startDate()).endDate(c.endDate()).notes(c.notes()).build();
 }
 public ContractFollowUp followUp(UUID user,UUID id) {branches.requireOwnedBranch(user,contracts.getBranchId(id));return followUp.get(id);}
 @Transactional public void updateFollowUp(UUID user,UUID id,FollowUpCommand c) {UUID branch=contracts.getBranchId(id);branches.requireOwnedBranch(user,branch);leadWorkflow.validateAdmin(user,branch,c.ownerId());followUp.update(id,user,c);}
 public OperationsPage tasks(UUID user,UUID branch,String kind,String status,String search,int page,int size) {
  if(!Set.of("ALL","LEAD","CONTRACT","TRIAL").contains(kind) || !Set.of("","TODAY","OVERDUE","UNASSIGNED","NO_CONTACT","NO_ACTION").contains(status))throw new BadRequestException("Некорректный фильтр задач");
  if(kind.equals("ALL")&&(!search.isBlank()||!Set.of("","TODAY","OVERDUE").contains(status)||page>200))throw new BadRequestException("Общая очередь поддерживает все задачи, сегодня и просроченные. Для поиска или следующих страниц выберите тип задач.");
  if(kind.equals("TRIAL")&&(!search.isBlank()||!Set.of("","TODAY","OVERDUE").contains(status)))throw new BadRequestException("Для пробных доступны все задачи, сегодня или просроченные");
  if(kind.equals("CONTRACT")&&!Set.of("","TODAY","OVERDUE","UNASSIGNED").contains(status))throw new BadRequestException("Некорректный фильтр сопровождения");
  var today=LocalDate.now(ZoneId.of("Asia/Almaty"));var q=company.scopedQuery(user,branch,search,status,today,today,page,size);
  OperationsPage result;
  if(kind.equals("ALL")) {
   int needed=Math.multiplyExact(page+1,size);var all=new CompanyQuery(q.branchIds(),"",status,q.dateFrom(),q.dateTo(),0,needed,true,"");
   var a=leads.tasks(all);var c=followUp.tasks(all);var t=trialReader.tasks(sessionReader.sessions(organization.groupBranches(q.branchIds()).keySet(),null,null).keySet(),all);
   var merged=new ArrayList<OperationsRow>();merged.addAll(a.content());merged.addAll(c.content());merged.addAll(t.content());
   merged.sort(Comparator.comparing(OperationsRow::dueAt,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(r->"URGENT".equals(r.priority())?0:"HIGH".equals(r.priority())?1:2).thenComparing(OperationsRow::kind).thenComparing(OperationsRow::id));
   long total=a.totalElements()+c.totalElements()+t.totalElements();result=new OperationsPage(merged.stream().skip((long)page*size).limit(size).toList(),total,(int)((total+size-1)/size),page,size,Map.of("LEAD",a.totalElements(),"CONTRACT",c.totalElements(),"TRIAL",t.totalElements()));
  } else result=switch(kind) {case "LEAD"->leads.tasks(q);case "CONTRACT"->followUp.tasks(q);default->trialReader.tasks(sessionReader.sessions(organization.groupBranches(q.branchIds()).keySet(),null,null).keySet(),q);};
  var names=admins.findAllByDispatcherId(user).stream().collect(Collectors.toMap(AdminDto::id,a->a.firstName()+" "+a.lastName()));
  return new OperationsPage(result.content().stream().map(r->r.toBuilder().ownerName(r.ownerId()==null?null:names.getOrDefault(r.ownerId(),"Ответственный недоступен")).build()).toList(),result.totalElements(),result.totalPages(),result.number(),result.size(),result.summary());
 }
 public TrialCompanyPage trials(UUID user,UUID branch,String status,LocalDate from,LocalDate to,int page,int size) {
  if(!Set.of("","SCHEDULED","COMPLETED","CANCELED","ATTENDED","NO_SHOW","UNMARKED","FOLLOW_UP","INTERESTED","NOT_INTERESTED","CONVERTED","PENDING").contains(status))throw new BadRequestException("Некорректный статус пробной");
  var q=company.scopedQuery(user,branch,"",status,from,to,page,size);
  return trialReader.page(sessionReader.sessions(organization.groupBranches(q.branchIds()).keySet(),q.filterDates()?q.dateFrom():null,q.filterDates()?q.dateTo():null).keySet(),q);
 }
 public TrialBookingDto requireTrial(UUID user,UUID id) {
  var trial=trials.getTrial(id);var session=sessions.getSessionDetails(trial.trainingSessionId());branches.requireOwnedBranch(user,groups.getGroupById(session.groupId()).branchId());
  if(trial.leadId()!=null)leadWorkflow.requireLead(user,trial.leadId());return trial;
 }
 public TrialWorkspace trial(UUID user,UUID id) {var t=requireTrial(user,id);return new TrialWorkspace(trials.getTrialDetails(id),t.version(),trialWorkflow.history(id));}
 @Transactional public void updateTrial(UUID user,UUID id,TrialMutationCommand c) {
  var t=requireTrial(user,id);
  if(c.operation()==TrialMutationCommand.Operation.ATTENDANCE && sessions.getSessionDetails(t.trainingSessionId()).startsAt().isAfter(LocalDateTime.now(ZoneId.of("Asia/Almaty"))))throw new BadRequestException("Посещение можно отметить после начала занятия");
  trialWorkflow.update(id,user,c);
  if(c.operation()==TrialMutationCommand.Operation.ATTENDANCE && t.leadId()!=null) {
   var lead=leadPort.getLeadOutput(t.leadId(),user);
   if(lead.status()==kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.TRIAL_SCHEDULED)
    leadPort.processEvent(t.leadId(),c.attendance()==kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus.ATTENDED?kz.edu.soccerhub.crm.application.state.LeadEvent.COMPLETE_TRIAL:kz.edu.soccerhub.crm.application.state.LeadEvent.NO_SHOW,null,null,user);
  }
 }
 public OperationsPage capacity(UUID user,UUID branch,String status,String search,int page,int size) {
  return capacity(user,branch,status,search,page,size,"");
 }
 public OperationsPage capacity(UUID user,UUID branch,String status,String search,int page,int size,String sort) {
  if(!Set.of("","ACTIVE","PAUSED","STOPPED","FULL","AVAILABLE").contains(status))throw new BadRequestException("Некорректный статус группы");
  var q=company.scopedQuery(user,branch,search,status,null,null,page,size).withSort(sort);var rows=organization.groups(q);
  var ids=rows.content().stream().map(OperationsRow::id).collect(Collectors.toSet());var today=LocalDate.now(ZoneId.of("Asia/Almaty"));
  var upcoming=coaches.getSessionsByGroups(ids,today,today.plusDays(14)).stream().filter(s->"PLANNED".equals(s.status())||"SCHEDULED".equals(s.status())).filter(s->s.scheduledStartAt()!=null&&s.scheduledStartAt().isAfter(LocalDateTime.now(ZoneId.of("Asia/Almaty")))).sorted(Comparator.comparing(s->s.scheduledStartAt())).toList();
  var coachIds=upcoming.stream().map(s->s.coachId()).filter(Objects::nonNull).collect(Collectors.toSet());
  var names=coachIds.isEmpty()?Map.<UUID,String>of():coaches.getCoaches(coachIds).stream().collect(Collectors.toMap(c->c.id(),c->c.firstName()+" "+c.lastName()+" · "+(c.workStatus()==null?"Статус не указан":switch(c.workStatus()){case AVAILABLE->"Доступен";case BUSY->"Занят";case VACATION->"В отпуске";})));
  return new OperationsPage(rows.content().stream().map(r->{var s=upcoming.stream().filter(t->r.id().equals(t.groupId())).findFirst().orElse(null);return r.toBuilder().startsAt(s==null?null:s.scheduledStartAt()).coachName(s==null?null:names.get(s.coachId())).build();}).toList(),rows.totalElements(),rows.totalPages(),rows.number(),rows.size(),organization.summary(q));
 }
 public OperationsReport report(UUID user,UUID branch,LocalDate from,LocalDate to) {
  var q=company.scopedQuery(user,branch,"","",from,to,0,25);long days=java.time.temporal.ChronoUnit.DAYS.between(q.dateFrom(),q.dateTo())+1;
  return new OperationsReport(company.summary(user,branch,from,to),q.filterDates()?company.summary(user,branch,q.dateFrom().minusDays(days),q.dateFrom().minusDays(1)):null,leads.report(q),payments.debtTrend(q),leads.lossReasonNames());
 }
 public StudentHistory student(UUID user,UUID id) {
  var q=company.scopedQuery(user,null,"","",null,null,0,25);var student=students.student(id,q.branchIds());
  var cs=contracts.getStudentContracts(student.branchId(),id).stream().map(c->contracts.getById(c.id())).toList();
  var ls=leads.studentLeads(id,q.branchIds()).stream().map(l->leadPort.getLeadOutput(l,user)).toList();
  var events=new ArrayList<TimelineEvent>();
  for(var l:ls)for(var a:leadPort.getLeadActivities(l.id()))events.add(new TimelineEvent(null,"LEAD",a.description(),a.createdAt()==null?null:a.createdAt().atZone(ZoneId.of("Asia/Almaty")).toInstant(),null));
  for(var c:cs) {events.addAll(followUp.get(c.id()).history());for(var h:c.history())events.add(new TimelineEvent(h.id(),"CONTRACT",h.type()+" · "+(h.comment()==null?"":h.comment()),h.createdAt().atZone(ZoneId.of("Asia/Almaty")).toInstant(),null));}
  var paymentRows=payments.payments(cs.stream().map(c->c.id()).collect(Collectors.toSet()));
  for(var p:paymentRows)events.add(new TimelineEvent(p.id(),"PAYMENT","Оплата · "+p.amount()+" "+p.currency()+" · "+(p.status().equals("PAID")?"Поступила":"Отменена")+(p.notes()==null?"":" · "+p.notes()),p.eventDate().atZone(ZoneId.of("Asia/Almaty")).toInstant(),null));
  var studentTrials=new LinkedHashMap<UUID,TrialBookingListItemDto>();
  trials.findList(TrialBookingSearchCommand.builder().studentId(id).build(),PageRequest.of(0,100,org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt").and(org.springframework.data.domain.Sort.by("id")))).forEach(t->studentTrials.put(t.id(),t));
  trials.findByLeadIds(ls.stream().map(l->l.id()).toList()).forEach(t->{if(id.equals(t.studentId())||ls.stream().flatMap(l->l.participants().stream()).anyMatch(p->id.equals(p.playerId())&&Objects.equals(p.id(),t.participantId())))studentTrials.put(t.id(),t);});
  var ts=studentTrials.values().stream().filter(t->q.branchIds().contains(groups.getGroupById(sessions.getSessionDetails(t.trainingSessionId()).groupId()).branchId())).sorted(Comparator.comparing(TrialBookingListItemDto::sessionStartsAt,Comparator.nullsLast(Comparator.reverseOrder()))).limit(100).toList();
  for(var t:ts)events.addAll(trialWorkflow.history(t.id()));
  events.sort(Comparator.comparing(TimelineEvent::occurredAt,Comparator.nullsLast(Comparator.reverseOrder())));
  return new StudentHistory(student,cs,paymentRows,ls,events.stream().limit(100).toList(),ts);
 }
}
