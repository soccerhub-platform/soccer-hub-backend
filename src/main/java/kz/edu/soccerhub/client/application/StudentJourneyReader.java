package kz.edu.soccerhub.client.application;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class StudentJourneyReader implements StudentJourneyPort {
 private final CompanyStudentHistoryPort students;
 private final ContractPort contracts;
 private final CompanyLeadOperationsPort leads;
 private final LeadPort leadPort;
 private final ContractFollowUpPort followUp;
 private final CompanyPaymentHistoryPort payments;
 private final TrialPort trials;
 private final GroupPort groups;
 private final TrialSessionPort sessions;
 private final TrialWorkflowPort trialWorkflow;
 public StudentHistory read(UUID user,Set<UUID> branchIds,UUID id) {
  var q=kz.edu.soccerhub.common.application.CompanyQueryFactory.create(branchIds,"","",null,null,0,25);var student=students.student(id,q.branchIds());
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
