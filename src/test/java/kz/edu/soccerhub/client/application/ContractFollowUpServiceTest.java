package kz.edu.soccerhub.client.application;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.common.application.*;
import kz.edu.soccerhub.common.port.CompanyContractBalancePort;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.junit.jupiter.api.*;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ContractFollowUpServiceTest {
 CompanyContractBalancePort balances;NamedParameterJdbcTemplate jdbc;ContractFollowUpService service;UUID id=UUID.randomUUID(),actor=UUID.randomUUID();
 @BeforeEach void setup() {
  var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");jdbc=new NamedParameterJdbcTemplate(ds);
  jdbc.getJdbcTemplate().execute("create table contracts(id uuid primary key,status varchar(20))");
  jdbc.getJdbcTemplate().execute("create table contract_follow_ups(contract_id uuid primary key,owner_id uuid,next_action varchar(240),due_at timestamp with time zone,outcome varchar(40),promised_payment_date date,last_contact_at timestamp with time zone,version bigint not null default 0)");
  jdbc.getJdbcTemplate().execute("create table contract_follow_up_history(id uuid primary key,contract_id uuid,actor_id uuid,type varchar(40),description varchar(4000),occurred_at timestamp with time zone)");
  jdbc.update("insert into contracts(id,status) values(:id,'ACTIVE')",Map.of("id",id));
  balances=mock(CompanyContractBalancePort.class);
  service=new ContractFollowUpService(jdbc,new OperationsProjectionReader(jdbc,new CompanyProjectionReader(jdbc)),balances,mock(kz.edu.soccerhub.common.port.AdminPort.class));
 }
 FollowUpCommand plan(long version) {return new FollowUpCommand(FollowUpCommand.Operation.PLAN,version,actor,"Позвонить",Instant.now().plusSeconds(7200),null,null,"Продление");}
 @Test void planContactAndCompletionPersistWithVersionAndHistory() {
  service.update(id,actor,plan(0));var f=service.get(id);assertEquals(1,f.version());assertEquals(actor,f.ownerId());assertEquals("Позвонить",f.nextAction());assertEquals(1,f.history().size());
  var promised=LocalDate.now(ZoneId.of("Asia/Almaty")).plusDays(2);
  service.update(id,actor,new FollowUpCommand(FollowUpCommand.Operation.CONTACT,1L,actor,"Проверить оплату",Instant.now().plusSeconds(14400),"PAYMENT_PROMISED",promised,"Клиент подтвердил"));
  f=service.get(id);assertEquals(promised,f.promisedPaymentDate());assertNotNull(f.lastContactAt());assertEquals(2,f.version());
  service.update(id,actor,new FollowUpCommand(FollowUpCommand.Operation.COMPLETE,2L,null,null,null,null,null,null));f=service.get(id);assertNull(f.nextAction());assertNull(f.dueAt());assertEquals(actor,f.ownerId());assertEquals(3,f.version());assertEquals(3,f.history().size());
 }
 @Test void staleEditorCannotOverwriteSavedPlan() {service.update(id,actor,plan(0));assertThrows(ConflictException.class,()->service.update(id,actor,plan(0)));assertEquals(1,service.get(id).version());assertEquals(1,service.get(id).history().size());}
 @Test void missingPromiseOrExpiredDeadlineDoesNotWrite() {
  assertThrows(BadRequestException.class,()->service.update(id,actor,new FollowUpCommand(FollowUpCommand.Operation.CONTACT,0L,null,null,null,"PAYMENT_PROMISED",null,null)));
  assertThrows(BadRequestException.class,()->service.update(id,actor,new FollowUpCommand(FollowUpCommand.Operation.PLAN,0L,null,"Звонок",Instant.now().minusSeconds(5),null,null,null)));
  assertEquals(0,service.get(id).version());assertTrue(service.get(id).history().isEmpty());
 }
 @Test void partialPaymentKeepsPromiseAndFullPaymentClosesExactlyOnceWithHistory() {
  service.update(id,actor,new FollowUpCommand(FollowUpCommand.Operation.CONTACT,0L,actor,"Проверить оплату",Instant.now().plusSeconds(14400),"PAYMENT_PROMISED",LocalDate.now(ZoneId.of("Asia/Almaty")).plusDays(1),"Обещал оплатить"));
  when(balances.outstandingContract(id)).thenReturn(new java.math.BigDecimal("1000"));service.paymentRecorded(id,actor);assertEquals(1,service.get(id).version());assertNotNull(service.get(id).nextAction());
  when(balances.outstandingContract(id)).thenReturn(java.math.BigDecimal.ZERO);service.paymentRecorded(id,actor);service.paymentRecorded(id,actor);
  var f=service.get(id);assertEquals(2,f.version());assertNull(f.nextAction());assertNull(f.dueAt());assertEquals("PAYMENT_RECEIVED",f.outcome());assertEquals(2,f.history().size());assertTrue(f.history().stream().anyMatch(e->e.type().equals("PAYMENT_RECEIVED")&&e.actorId().equals(actor)));
 }
 @Test void newRenewalPlanIsNotClosedByPayment() {
  service.update(id,actor,new FollowUpCommand(FollowUpCommand.Operation.CONTACT,0L,actor,"Оплата",Instant.now().plusSeconds(14400),"PAYMENT_PROMISED",LocalDate.now(ZoneId.of("Asia/Almaty")).plusDays(1),null));
  service.update(id,actor,plan(1));service.paymentRecorded(id,actor);assertEquals("Позвонить",service.get(id).nextAction());assertEquals(2,service.get(id).history().size());verifyNoInteractions(balances);
 }
 @Test void cancelledContractCannotBeFollowedUp() {jdbc.update("update contracts set status='CANCELLED' where id=:id",Map.of("id",id));assertThrows(BadRequestException.class,()->service.update(id,actor,plan(0)));assertEquals(0,service.get(id).version());}
}
