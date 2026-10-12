package kz.edu.soccerhub.dispatcher.application.service;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class DispatcherCompanyService {
 private final DispatcherClubService clubs;
 private final BranchPort branches;
 private final CompanyClientReadPort clients;
 private final CompanyPaymentReadPort payments;
 private final CompanyLeadReadPort leads;
 public List<BranchDto> scope(UUID user, UUID branch) {
  var clubIds=clubs.getAll(user);
  var owned=clubIds.isEmpty()? List.<BranchDto>of():new ArrayList<>(branches.findByClubIds(clubIds));
  if(branch!=null && owned.stream().noneMatch(b->b.id().equals(branch))) throw new BadRequestException("Нет доступа к филиалу",branch);
  return owned.stream().filter(b->branch==null || b.id().equals(branch)).toList();
 }
 private CompanyQuery query(List<BranchDto> scope,String search,String status,LocalDate from,LocalDate to,int page,int size) {
  boolean filterDates=from!=null || to!=null;
  LocalDate today=LocalDate.now(ZoneId.of("Asia/Almaty"));
  if(filterDates && (from==null || to==null)) throw new BadRequestException("Укажите обе даты периода");
  from=from==null?today.withDayOfMonth(1):from;to=to==null?today:to;
  if(from.isAfter(to) || java.time.temporal.ChronoUnit.DAYS.between(from,to)>366) throw new BadRequestException("Выберите период до 367 дней с корректными датами");
  if(page<0 || page>100000 || size<1 || size>100) throw new BadRequestException("Некорректные параметры страницы");
  if(search!=null && search.length()>200) throw new BadRequestException("Поисковый запрос слишком длинный");
  return new CompanyQuery(scope.stream().map(BranchDto::id).collect(Collectors.toSet()),search==null?"":search.trim(),status==null?"":status,from,to,page,size,filterDates,"");
 }
 public CompanyQuery scopedQuery(UUID user,UUID branch,String search,String status,LocalDate from,LocalDate to,int page,int size) {return query(scope(user,branch),search,status,from,to,page,size);}
 public CompanySummary summary(UUID user,UUID branch,LocalDate from,LocalDate to) {
  var scope=scope(user,branch);var q=query(scope,"","",from,to,0,25);
  var metrics=new ArrayList<CompanyMetric>();metrics.addAll(clients.metrics(q));metrics.addAll(payments.metrics(q));metrics.addAll(leads.metrics(q));
  return new CompanySummary(scope,metrics,q.filterDates()?q.dateFrom():null,q.filterDates()?q.dateTo():null);
 }
 public CompanyPage page(UUID user,String kind,UUID branch,String search,String status,LocalDate from,LocalDate to,int page,int size,String attention) {
  return page(user,kind,branch,search,status,from,to,page,size,attention,"");
 }
 public CompanyPage page(UUID user,String kind,UUID branch,String search,String status,LocalDate from,LocalDate to,int page,int size,String attention,String sort) {
  attention=attention==null?"":attention;
  if(!attention.isEmpty() && !(kind.equals("leads") && Set.of("UNASSIGNED","OVERDUE","NO_CONTACT","NO_ACTION").contains(attention)) && !(kind.equals("contracts") && attention.equals("ENDING"))) throw new BadRequestException("Некорректный фильтр внимания");
  var base=query(scope(user,branch),search,status,from,to,page,size);
  var q=new CompanyQuery(base.branchIds(),base.search(),base.status(),base.dateFrom(),base.dateTo(),base.page(),base.size(),base.filterDates(),attention,sort);
  return switch(kind) {
   case "students" -> clients.page(q,false);
   case "contracts" -> clients.page(q,true);
   case "debts" -> payments.debts(q);
   case "payments" -> payments.page(q);
   case "leads" -> leads.page(q);
   default -> throw new BadRequestException("Неизвестный раздел");
  };
 }
}
