package kz.edu.soccerhub.client.application;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.port.ContractFollowUpPort;
import kz.edu.soccerhub.common.application.OperationsProjectionReader;
import kz.edu.soccerhub.common.exception.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.time.*;
@Service @RequiredArgsConstructor
public class ContractFollowUpService implements ContractFollowUpPort {
 private final NamedParameterJdbcTemplate jdbc;
 private final OperationsProjectionReader reader;
 private final kz.edu.soccerhub.common.port.CompanyContractBalancePort balances;
 private final kz.edu.soccerhub.common.port.AdminPort admins;
 @Override @Transactional(readOnly=true) public ContractFollowUp get(UUID id) {
  var p=new MapSqlParameterSource("id",id);
  var rows=jdbc.query("select * from contract_follow_ups where contract_id=:id",p,(r,n)->ContractFollowUp.builder().contractId(id).ownerId(r.getObject("owner_id",UUID.class)).nextAction(r.getString("next_action")).dueAt(r.getTimestamp("due_at")==null?null:r.getTimestamp("due_at").toInstant()).outcome(r.getString("outcome")).promisedPaymentDate(r.getObject("promised_payment_date",LocalDate.class)).lastContactAt(r.getTimestamp("last_contact_at")==null?null:r.getTimestamp("last_contact_at").toInstant()).version(r.getLong("version")).build());
  var history=jdbc.query("select * from contract_follow_up_history where contract_id=:id order by occurred_at desc,id desc limit 100",p,(r,n)->new TimelineEvent(r.getObject("id",UUID.class),r.getString("type"),r.getString("description"),r.getTimestamp("occurred_at").toInstant(),r.getObject("actor_id",UUID.class)));
  var v=rows.isEmpty()?ContractFollowUp.builder().contractId(id).version(0).build():rows.getFirst();
  return new ContractFollowUp(id,v.ownerId(),v.nextAction(),v.dueAt(),v.outcome(),v.promisedPaymentDate(),v.lastContactAt(),v.version(),history);
 }
 @Override @Transactional public void update(UUID id,UUID actor,FollowUpCommand c) {
  var p=new MapSqlParameterSource("id",id);
  var statuses=jdbc.queryForList("select status from contracts where id=:id for update",p,String.class);
  if(statuses.isEmpty())throw new NotFoundException("Договор не найден",id);
  if(Set.of("DRAFT","CANCELLED").contains(statuses.getFirst()))throw new BadRequestException("Сопровождение доступно после активации договора");
  var old=get(id);
  if(c.version()==null || c.version()!=old.version())throw new ConflictException("Сопровождение уже изменено. Обновите карточку.","FOLLOW_UP_CONFLICT",Map.of("contractId",id));
  if(c.operation()==null)throw new BadRequestException("Укажите действие");
  String action=c.nextAction()==null?"":c.nextAction().trim();
  if(c.operation()!=FollowUpCommand.Operation.COMPLETE && (action.isEmpty()!=(c.dueAt()==null)))throw new BadRequestException("Укажите действие и срок вместе");
  if(c.operation()==FollowUpCommand.Operation.PLAN && action.isEmpty())throw new BadRequestException("Укажите следующее действие");
  if(c.operation()==FollowUpCommand.Operation.COMPLETE && old.nextAction()==null)throw new BadRequestException("Нет действия для завершения");
  if(c.operation()!=FollowUpCommand.Operation.COMPLETE && c.dueAt()!=null && !c.dueAt().isAfter(Instant.now()) && !c.dueAt().equals(old.dueAt()))throw new BadRequestException("Новый срок должен быть в будущем");
  boolean contact=c.operation()==FollowUpCommand.Operation.CONTACT,complete=c.operation()==FollowUpCommand.Operation.COMPLETE;
  if(contact && !Set.of("REACHED","NO_ANSWER","FOLLOW_UP","WRONG_NUMBER","PAYMENT_PROMISED").contains(c.outcome()==null?"":c.outcome()))throw new BadRequestException("Укажите результат контакта");
  if(contact && "PAYMENT_PROMISED".equals(c.outcome()) && c.promisedPaymentDate()==null)throw new BadRequestException("Укажите обещанную дату оплаты");
  if(contact && c.promisedPaymentDate()!=null && c.promisedPaymentDate().isBefore(LocalDate.now(ZoneId.of("Asia/Almaty"))))throw new BadRequestException("Обещанная дата оплаты не может быть в прошлом");
  p.addValue("owner",complete?old.ownerId():c.ownerId()).addValue("action",complete||action.isEmpty()?null:action).addValue("due",complete?null:c.dueAt()==null?null:java.sql.Timestamp.from(c.dueAt()))
   .addValue("outcome",contact?c.outcome():c.operation()==FollowUpCommand.Operation.PLAN?null:old.outcome()).addValue("promised",contact?c.promisedPaymentDate():c.operation()==FollowUpCommand.Operation.PLAN?null:old.promisedPaymentDate()).addValue("contact",contact?java.sql.Timestamp.from(Instant.now()):old.lastContactAt()==null?null:java.sql.Timestamp.from(old.lastContactAt())).addValue("version",old.version()+1);
  if(old.version()==0)jdbc.update("insert into contract_follow_ups(contract_id,version) values(:id,0)",p);
  jdbc.update("update contract_follow_ups set owner_id=:owner,next_action=:action,due_at=:due,outcome=:outcome,promised_payment_date=:promised,last_contact_at=:contact,version=:version where contract_id=:id",p);
  String description=complete?"Выполнено: "+old.nextAction():contact?"Контакт · "+outcomeLabel(c.outcome()):"План сопровождения";
  if(!complete && !action.isEmpty())description+=" · "+action+" · срок: "+java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Asia/Almaty")).format(c.dueAt());
  if(contact && c.promisedPaymentDate()!=null)description+=" · обещана оплата: "+c.promisedPaymentDate();
  UUID owner=(UUID)p.getValue("owner");
  description+=" · ответственный: "+(owner==null?"Не назначен":admins.findById(owner).map(a->a.firstName()+" "+a.lastName()).orElse("Администратор недоступен"));
  if(c.comment()!=null && !c.comment().isBlank())description+="\n"+c.comment().trim();
  p.addValue("eventId",UUID.randomUUID()).addValue("actor",actor).addValue("type",c.operation().name()).addValue("description",description).addValue("now",java.sql.Timestamp.from(Instant.now()));
  jdbc.update("insert into contract_follow_up_history(id,contract_id,actor_id,type,description,occurred_at) values(:eventId,:id,:actor,:type,:description,:now)",p);
 }
 @Override @Transactional public void lockForPayment(UUID id) {
  var existing=jdbc.queryForList("select id from contracts where id=:id for update",new MapSqlParameterSource("id",id),UUID.class);
  if(existing.isEmpty())throw new NotFoundException("Договор не найден",id);
 }
 @Override @Transactional public void paymentRecorded(UUID id,UUID actor) {
  var p=new MapSqlParameterSource("id",id);
  var existing=jdbc.queryForList("select id from contracts where id=:id for update",p,UUID.class);
  if(existing.isEmpty())throw new NotFoundException("Договор не найден",id);
  var current=get(id);
  if(!"PAYMENT_PROMISED".equals(current.outcome()) || current.nextAction()==null)return;
  var remaining=balances.outstandingContract(id);
  if(remaining==null || remaining.signum()>0)return;
  int changed=jdbc.update("update contract_follow_ups set next_action=null,due_at=null,outcome='PAYMENT_RECEIVED',version=version+1 where contract_id=:id and outcome='PAYMENT_PROMISED' and next_action is not null",p);
  if(changed==0)return;
  p.addValue("eventId",UUID.randomUUID()).addValue("actor",actor).addValue("now",java.sql.Timestamp.from(Instant.now()));
  jdbc.update("insert into contract_follow_up_history(id,contract_id,actor_id,type,description,occurred_at) values(:eventId,:id,:actor,'PAYMENT_RECEIVED','Оплата получена полностью · обещание оплаты выполнено',:now)",p);
 }
 private String outcomeLabel(String value) {return switch(value) {case "REACHED"->"Связались";case "NO_ANSWER"->"Нет ответа";case "FOLLOW_UP"->"Повторный контакт";case "WRONG_NUMBER"->"Неверный номер";case "PAYMENT_PROMISED"->"Обещана оплата";default->value;};}
 @Override public Map<String,Long> summary(CompanyQuery q) {
  var today=LocalDate.now(ZoneId.of("Asia/Almaty"));
  var debts=balances.debtContracts(q.branchIds());var p=new MapSqlParameterSource("ids",q.branchIds()).addValue("today",today).addValue("soon",today.plusDays(14)).addValue("debts",debts.isEmpty()?Set.of(new UUID(0,0)):debts.keySet());
  return jdbc.queryForObject("""
select count(case when c.id in (:debts) and f.outcome='PAYMENT_PROMISED' and f.promised_payment_date=:today then 1 end) promised,
count(case when c.start_date<=:today and c.end_date between :today and :soon then 1 end) ending
from contracts c join client_profiles cl on cl.id=c.client_id left join contract_follow_ups f on f.contract_id=c.id
where cl.branch_id in (:ids) and c.status not in ('DRAFT','CANCELLED')
""",p,(rs,n)->Map.of("promisedToday",rs.getLong("promised"),"endingSoon",rs.getLong("ending")));
 }
 @Override public OperationsPage tasks(CompanyQuery q) {
  var debts=balances.debtContracts(q.branchIds());
  var debtIds=debts.isEmpty()?Set.of(new UUID(0,0)):debts.keySet();
  var result=reader.page(q,"""
   select c.id,cl.branch_id,'CONTRACT' as kind,concat_ws(' ',p.first_name,p.last_name) as name,concat_ws(' ',cl.first_name,cl.last_name) as contact,cl.phone,'OPEN' as status,f.owner_id,
    coalesce(f.next_action,case when c.id in (:debtIds) then 'Связаться по задолженности' else 'Обсудить продление договора' end) as next_action,
    coalesce(f.due_at,case when c.id not in (:debtIds) then c.end_date::timestamp at time zone 'Asia/Almaty' else null end) as due_at,
    case when c.id in (:debtIds) then 'HIGH' else 'NORMAL' end as priority,c.contract_number as reference,null::integer as occupied,null::integer as capacity,null::timestamp as starts_at,null::text as coach_name,null::numeric as amount,c.currency
   from contracts c left join contract_follow_ups f on f.contract_id=c.id join client_profiles cl on cl.id=c.client_id left join players p on p.id=c.player_id
   where cl.branch_id in (:ids) and c.status not in ('DRAFT','CANCELLED')
    and (f.next_action is not null or c.id in (:debtIds) or (c.start_date<=:today and c.end_date between :today and :soon))
    and (:status='' or (:status='OVERDUE' and f.due_at<:now) or (:status='TODAY' and f.due_at>=:dayFrom and f.due_at<:dayUntil) or (:status='UNASSIGNED' and f.owner_id is null))
    and lower(concat_ws(' ',c.contract_number,cl.first_name,cl.last_name,cl.phone,p.first_name,p.last_name,f.next_action)) like :search escape '!'
   ""","due_at asc nulls last,case priority when 'HIGH' then 1 else 2 end,id",Map.of("debtIds",debtIds));
  return new OperationsPage(result.content().stream().map(r->r.toBuilder().amount(debts.get(r.id())).build()).toList(),result.totalElements(),result.totalPages(),result.number(),result.size());
 }
}
