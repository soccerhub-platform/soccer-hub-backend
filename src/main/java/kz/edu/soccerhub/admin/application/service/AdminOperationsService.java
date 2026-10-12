package kz.edu.soccerhub.admin.application.service;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.admin.application.dto.operations.AdminFollowUpInput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.common.application.CompanyQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AdminOperationsService {
 private final AdminBranchAccessPort access;
 private final AdminPort admins;
 private final BranchPort branches;
 private final CompanyLeadOperationsPort leads;
 private final ContractFollowUpPort followUps;
 private final ContractPort contracts;
 private final CompanyOrganizationPort organization;
 private final CompanySessionReadPort sessions;
 private final CompanyTrialReadPort trials;
 private final CompanyClientReadPort clients;
 private final CompanyPaymentReadPort payments;
 private final CompanyLeadReadPort leadMetrics;
 private final CompanyPaymentHistoryPort paymentHistory;
 private final StudentJourneyPort journey;
 public StudentHistory student(UUID actor,UUID branch,UUID id) {requireBranch(actor,branch);return journey.read(actor,Set.of(branch),id); }
 public void requireBranch(UUID actor,UUID branch) {
  if(branch==null||!access.verifyAdminBelongsToBranch(actor,branch))throw new ForbiddenException("Нет доступа к филиалу");
 }
 public CompanyQuery query(UUID actor,UUID branch,String search,String status,LocalDate from,LocalDate to,int page,int size) {
  requireBranch(actor,branch);return CompanyQueryFactory.create(Set.of(branch),search,status,from,to,page,size);
 }
 public Collection<AdminDto> assignees(UUID actor,UUID branch) {requireBranch(actor,branch);return admins.findAllByBranchId(branch).stream().filter(AdminDto::isActive).toList();}
 public ContractFollowUp followUp(UUID actor,UUID contract) {requireBranch(actor,contracts.getBranchId(contract));return followUps.get(contract);}
 @Transactional public void updateFollowUp(UUID actor,UUID contract,AdminFollowUpInput input) {
  updateFollowUp(actor,contract,input.toCommand());
 }
 @Transactional public void updateFollowUp(UUID actor,UUID contract,FollowUpCommand c) {
  UUID branch=contracts.getBranchId(contract);requireBranch(actor,branch);
  if(c.ownerId()!=null&&assignees(actor,branch).stream().noneMatch(a->a.id().equals(c.ownerId())))throw new BadRequestException("Ответственный недоступен в филиале");
  followUps.update(contract,actor,c);
 }
 private Set<UUID> sessionIds(CompanyQuery q) {return sessions.sessions(organization.groupBranches(q.branchIds()).keySet(),null,null).keySet();}
 public OperationsPage tasks(UUID actor,UUID branch,String kind,String status,String search,boolean mine,int page,int size) {
  if(!Set.of("ALL","LEAD","CONTRACT","TRIAL").contains(kind)||!Set.of("","TODAY","OVERDUE","UNASSIGNED","NO_CONTACT","NO_ACTION").contains(status))throw new BadRequestException("Некорректный фильтр");
  if(kind.equals("ALL")&&(!search.isBlank()||!Set.of("","TODAY","OVERDUE").contains(status)||page>200))throw new BadRequestException("Для поиска выберите категорию задач");
  if(kind.equals("TRIAL")&&(!search.isBlank()||!Set.of("","TODAY","OVERDUE").contains(status)))throw new BadRequestException("Недопустимый фильтр пробных");
  if(kind.equals("CONTRACT")&&!Set.of("","TODAY","OVERDUE","UNASSIGNED").contains(status))throw new BadRequestException("Недопустимый фильтр договоров");
  var today=LocalDate.now(ZoneId.of("Asia/Almaty"));var q=query(actor,branch,search,status,today,today,page,size).withOwner(mine?actor:null);
  OperationsPage result;
  if(kind.equals("ALL")) {
   int needed=Math.multiplyExact(page+1,size);var all=new CompanyQuery(q.branchIds(),"",status,today,today,0,needed,true,"").withOwner(q.ownerId());
   var l=leads.tasks(all);var c=followUps.tasks(all);var t=trials.tasks(sessionIds(all),all);
   var rows=new ArrayList<OperationsRow>();rows.addAll(l.content());rows.addAll(c.content());rows.addAll(t.content());
   rows.sort(Comparator.comparing(OperationsRow::dueAt,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(r->"URGENT".equals(r.priority())?0:"HIGH".equals(r.priority())?1:2).thenComparing(OperationsRow::kind).thenComparing(OperationsRow::id));
   long total=l.totalElements()+c.totalElements()+t.totalElements();result=new OperationsPage(rows.stream().skip((long)page*size).limit(size).toList(),total,(int)((total+size-1)/size),page,size,Map.of("LEAD",l.totalElements(),"CONTRACT",c.totalElements(),"TRIAL",t.totalElements()));
  } else result=switch(kind) {case "LEAD"->leads.tasks(q);case "CONTRACT"->followUps.tasks(q);default->trials.tasks(sessionIds(q),q);};
  var names=assignees(actor,branch).stream().collect(Collectors.toMap(AdminDto::id,a->a.firstName()+" "+a.lastName()));
  return new OperationsPage(result.content().stream().map(r->r.toBuilder().ownerName(r.ownerId()==null?null:names.getOrDefault(r.ownerId(),"Недоступен")).build()).toList(),result.totalElements(),result.totalPages(),page,size,result.summary());
 }
 public TrialCompanyPage trials(UUID actor,UUID branch,String status,LocalDate from,LocalDate to,String sort,int page,int size) {
  if(!Set.of("","SCHEDULED","COMPLETED","CANCELED","ATTENDED","NO_SHOW","UNMARKED","FOLLOW_UP","INTERESTED","NOT_INTERESTED","CONVERTED","PENDING").contains(status)||!Set.of("NEWEST","OLDEST").contains(sort))throw new BadRequestException("Некорректный фильтр пробных");
  var q=query(actor,branch,"",status,from,to,page,size).withSort(sort);
  var ids=sessions.sessions(organization.groupBranches(q.branchIds()).keySet(),q.filterDates()?q.dateFrom():null,q.filterDates()?q.dateTo():null).keySet();
  return trials.page(ids,q);
 }
 public OperationsPage capacity(UUID actor,UUID branch,String status,String search,String sort,int page,int size) {
  if(!Set.of("","ACTIVE","PAUSED","STOPPED","FULL","AVAILABLE").contains(status))throw new BadRequestException("Недопустимый статус");
  var q=query(actor,branch,search,status,null,null,page,size).withSort(sort);var rows=organization.groups(q);
  return new OperationsPage(rows.content(),rows.totalElements(),rows.totalPages(),page,size,organization.summary(q));
 }
 public kz.edu.soccerhub.admin.application.dto.operations.AdminDebtPage debts(UUID actor,UUID branch,String search,String status,String sort,int page,int size) {
  if(!Set.of("","ACTIVE","EXPIRED").contains(status))throw new BadRequestException("Недопустимый статус договора");
  var q=query(actor,branch,search,status,null,null,page,size).withSort(sort);var data=payments.debts(q);
  return new kz.edu.soccerhub.admin.application.dto.operations.AdminDebtPage(data.content(),data.totalElements(),data.totalPages(),data.number(),data.size(),payments.debtMetrics(q),followUps.summary(q));
 }
 private CompanySummary summary(UUID branch,CompanyQuery q) {
  var metrics=new ArrayList<CompanyMetric>();metrics.addAll(clients.metrics(q));metrics.addAll(payments.metrics(q));metrics.addAll(leadMetrics.metrics(q));
  var groupBranches=organization.groupBranches(q.branchIds());
  metrics.addAll(sessions.attendanceMetrics(groupBranches,q));
  var scopedSessions=sessions.sessions(groupBranches.keySet(),q.filterDates()?q.dateFrom():null,q.filterDates()?q.dateTo():null).keySet();
  trials.summary(scopedSessions,q).forEach((key,count)->metrics.add(new CompanyMetric(branch,"trial:"+key,count,null,null)));
  return new CompanySummary(new ArrayList<>(branches.findAllByIds(Set.of(branch))),metrics,q.filterDates()?q.dateFrom():null,q.filterDates()?q.dateTo():null);
 }
 public OperationsReport report(UUID actor,UUID branch,LocalDate from,LocalDate to) {
  var q=query(actor,branch,"","",from,to,0,25);long days=java.time.temporal.ChronoUnit.DAYS.between(q.dateFrom(),q.dateTo())+1;
  var previous=q.filterDates()?CompanyQueryFactory.create(Set.of(branch),"","",q.dateFrom().minusDays(days),q.dateFrom().minusDays(1),0,25):null;
  return new OperationsReport(summary(branch,q),previous==null?null:summary(branch,previous),leads.report(q),paymentHistory.debtTrend(q),leads.lossReasonNames());
 }
}
