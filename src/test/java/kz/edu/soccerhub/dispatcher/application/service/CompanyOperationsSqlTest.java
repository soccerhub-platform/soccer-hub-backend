package kz.edu.soccerhub.dispatcher.application.service;
import kz.edu.soccerhub.common.application.*;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.client.application.*;
import kz.edu.soccerhub.crm.application.service.CompanyLeadOperationsReader;
import kz.edu.soccerhub.organization.application.service.CompanyOrganizationReader;
import kz.edu.soccerhub.payments.application.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.junit.jupiter.api.*;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class CompanyOperationsSqlTest {
 NamedParameterJdbcTemplate jdbc;CompanyProjectionReader projections;OperationsProjectionReader operations;
 UUID owned=UUID.randomUUID(),foreign=UUID.randomUUID(),client=UUID.randomUUID(),player=UUID.randomUUID(),contract=UUID.randomUUID(),lead=UUID.randomUUID(),group=UUID.randomUUID();
 @BeforeEach void setup() {
  jdbc=new NamedParameterJdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=KEY","sa",""));projections=new CompanyProjectionReader(jdbc);operations=new OperationsProjectionReader(jdbc,projections);
  for(String sql:List.of(
   "create domain timestamptz as timestamp with time zone",
   "create table client_profiles(id uuid primary key,branch_id uuid,first_name varchar,last_name varchar,phone varchar,email varchar,status varchar)",
   "create table players(id uuid primary key,parent_id uuid,first_name varchar,last_name varchar,birth_date date)",
   "create table contracts(id uuid primary key,client_id uuid,player_id uuid,status varchar,contract_number varchar,amount numeric,currency varchar,start_date date,end_date date)",
   "create table payments(id uuid primary key,contract_id uuid,branch_id uuid,player_id uuid,amount numeric,currency varchar,status varchar,paid_at timestamp,comment varchar)",
   "create table contract_follow_ups(contract_id uuid primary key,owner_id uuid,next_action varchar,due_at timestamptz,outcome varchar,promised_payment_date date,last_contact_at timestamptz,version bigint)",
   "create table groups(id uuid primary key,branch_id uuid,name varchar,status varchar,capacity int)",
   "create table group_memberships(id uuid primary key,group_id uuid,player_id uuid,status varchar,joined_at date,left_at date)",
   "create table leads(id uuid primary key,branch_id uuid,primary_contact_name varchar,primary_contact_phone varchar,assigned_admin_id uuid,status varchar,next_action varchar,next_action_at timestamptz,work_priority varchar,last_contact_at timestamptz,created_at timestamp,lost_at timestamp,lost_reason_code varchar)",
   "create table lead_activities(id uuid primary key,lead_id uuid,activity_type varchar,details varchar,created_at timestamp)",
   "create table lead_participants(id uuid primary key,lead_id uuid,player_id uuid)"
  ))jdbc.getJdbcTemplate().execute(sql);
  jdbc.update("insert into client_profiles values(:c,:b,'Анна','Иванова','777',null,'ACTIVE')",Map.of("c",client,"b",owned));
  jdbc.update("insert into players values(:p,:c,'Иван','Иванов',date '2015-01-01')",Map.of("p",player,"c",client));
  var today=LocalDate.now(ZoneId.of("Asia/Almaty"));
  jdbc.update("insert into contracts values(:id,:c,:p,'ACTIVE','42',10000,'KZT',:start,:end)",Map.of("id",contract,"c",client,"p",player,"start",today.minusDays(15),"end",today.plusDays(5)));
  jdbc.update("insert into groups values(:id,:b,'Группа 100%','ACTIVE',1)",Map.of("id",group,"b",owned));
  jdbc.update("insert into group_memberships values(:id,:g,:p,'UPCOMING',:start,:end)",Map.of("id",UUID.randomUUID(),"g",group,"p",player,"start",today,"end",today));
  jdbc.update("insert into groups values(:id,:b,'Чужая','ACTIVE',20)",Map.of("id",UUID.randomUUID(),"b",foreign));
  jdbc.update("insert into leads(id,branch_id,primary_contact_name,primary_contact_phone,status,work_priority,created_at) values(:id,:b,'Анна','777','NEW','NORMAL',:created)",Map.of("id",lead,"b",owned,"created",today.atTime(10,0)));
  jdbc.update("insert into leads(id,branch_id,primary_contact_name,status,work_priority,created_at) values(:id,:b,'Чужой','NEW','NORMAL',:created)",Map.of("id",UUID.randomUUID(),"b",foreign,"created",today.atTime(10,0)));
 }
 CompanyQuery query(String search,String status) {var today=LocalDate.now(ZoneId.of("Asia/Almaty"));return new CompanyQuery(Set.of(owned),search,status,today,today,0,25,true,"");}
 @Test void leadQueueRestrictsScopeAndShowsMissingWork() {
  var reader=new CompanyLeadOperationsReader(operations,projections,jdbc);var result=reader.tasks(query("","NO_CONTACT"));assertEquals(1,result.totalElements());assertEquals(lead,result.content().getFirst().id());assertEquals("Первый контакт",result.content().getFirst().nextAction());
  assertTrue(reader.studentLeads(player,Set.of(owned)).isEmpty());
 }
 @Test void capacityCountsUpcomingMembershipOnInclusiveEndDateAndEscapesSearch() {
  var reader=new CompanyOrganizationReader(operations,jdbc);var q=query("100%","");var rows=reader.groups(q);assertEquals(1,rows.totalElements());assertEquals(1,rows.content().getFirst().occupied());assertEquals(1,reader.summary(q).get("full"));assertEquals(0,reader.summary(q).get("available"));assertEquals(Map.of(group,owned),reader.groupBranches(Set.of(owned)));
 }
 @Test void debtAndRenewalAppearBeforeManualPlanningAndIgnoreCancelledPayments() {
  var balances=new CompanyContractBalanceReader(jdbc);assertEquals(10000,balances.debtContracts(Set.of(owned)).get(contract).intValue());
  var follow=new ContractFollowUpService(jdbc,operations,balances,org.mockito.Mockito.mock(kz.edu.soccerhub.common.port.AdminPort.class));var result=follow.tasks(query("",""));assertEquals(1,result.totalElements());assertEquals("Связаться по задолженности",result.content().getFirst().nextAction());assertEquals(10000,result.content().getFirst().amount().intValue());
  jdbc.update("insert into payments values(:id,:c,:b,:p,10000,'KZT','CANCELLED',:at,null)",Map.of("id",UUID.randomUUID(),"c",contract,"b",owned,"p",player,"at",LocalDateTime.now()));assertEquals(1,balances.debtContracts(Set.of(owned)).size());
  jdbc.update("update payments set status='PAID'",Map.of());result=follow.tasks(query("",""));assertEquals("Обсудить продление договора",result.content().getFirst().nextAction());assertNotNull(result.content().getFirst().dueAt());
 }
 @Test void responseReportUsesFirstRecordedContactAndStudentLinksUseIds() {
  var reader=new CompanyLeadOperationsReader(operations,projections,jdbc);var today=LocalDate.now(ZoneId.of("Asia/Almaty"));
  jdbc.update("insert into lead_activities values(:id,:l,'WORK_UPDATED','Контакт · Телефон',:at)",Map.of("id",UUID.randomUUID(),"l",lead,"at",today.atTime(10,30)));
  jdbc.update("insert into lead_activities values(:id,:l,'WORK_UPDATED','Контакт · Повторный',:at)",Map.of("id",UUID.randomUUID(),"l",lead,"at",today.atTime(11,30)));
  var metric=reader.report(query("","")).stream().filter(m->m.key().equals("responseMinutes")).findFirst().orElseThrow();assertEquals(1,metric.count());assertEquals(30,metric.amount().intValue());
  jdbc.update("insert into lead_participants values(:id,:l,:p)",Map.of("id",UUID.randomUUID(),"l",lead,"p",player));assertEquals(List.of(lead),reader.studentLeads(player,Set.of(owned)));assertTrue(reader.studentLeads(UUID.randomUUID(),Set.of(owned)).isEmpty());
 }

 @Test void allTimeReportIncludesOlderLeadsButOnlyActualLosses() {
  var old=LocalDate.now(ZoneId.of("Asia/Almaty")).minusYears(3).atTime(10,0);
  jdbc.update("insert into leads(id,branch_id,primary_contact_name,status,created_at,lost_at,lost_reason_code) values(:id,:b,'Старая заявка','LOST',:at,:at,'PRICE')",Map.of("id",UUID.randomUUID(),"b",owned,"at",old));
  var current=query("","");var all=new CompanyQuery(current.branchIds(),"","",current.dateFrom(),current.dateTo(),0,25,false,"");
  var reader=new CompanyLeadOperationsReader(operations,projections,jdbc);
  assertEquals(1,reader.report(current).stream().filter(m->m.key().startsWith("stage:")).mapToLong(CompanyMetric::count).sum());
  assertEquals(2,reader.report(all).stream().filter(m->m.key().startsWith("stage:")).mapToLong(CompanyMetric::count).sum());
  assertEquals(1,reader.report(all).stream().filter(m->m.key().startsWith("loss:")).mapToLong(CompanyMetric::count).sum());
  var leads=new kz.edu.soccerhub.crm.application.service.CompanyLeadReader(projections,jdbc,org.mockito.Mockito.mock(kz.edu.soccerhub.common.port.AdminPort.class));
  assertEquals(2,leads.metrics(all).stream().filter(m->m.key().equals("newLeads")).mapToLong(CompanyMetric::count).sum());
  jdbc.update("insert into payments values(:id,:c,:b,:p,500,'KZT','PAID',:at,null)",Map.of("id",UUID.randomUUID(),"c",contract,"b",owned,"p",player,"at",old));
  var payments=new CompanyPaymentReader(projections);
  assertFalse(payments.metrics(current).stream().anyMatch(m->m.key().equals("receipts")));
  assertEquals(500,payments.metrics(all).stream().filter(m->m.key().equals("receipts")).findFirst().orElseThrow().amount().intValue());
 }
 @Test void groupSortAppliesBeforePaginationAndRejectsSqlInput() {
  UUID other=UUID.randomUUID();jdbc.update("insert into groups values(:id,:b,'Академия','ACTIVE',30)",Map.of("id",other,"b",owned));
  var q=query("","");var single=new CompanyQuery(q.branchIds(),"","",q.dateFrom(),q.dateTo(),0,1,false,"","AVAILABLE");
  var reader=new CompanyOrganizationReader(operations,jdbc);
  assertEquals(other,reader.groups(single).content().getFirst().id());
  assertEquals(group,reader.groups(single.withSort("FULL")).content().getFirst().id());
  assertThrows(kz.edu.soccerhub.common.exception.BadRequestException.class,()->reader.groups(single.withSort("name; drop table groups")));
 }
 @Test void ownerFilterIsAppliedBeforeCountAndPagination() {
  var owner=UUID.randomUUID();var other=UUID.randomUUID();var second=UUID.randomUUID();var today=LocalDate.now(ZoneId.of("Asia/Almaty"));
  jdbc.update("update leads set assigned_admin_id=:owner,next_action_at=:at where id=:id",Map.of("id",lead,"owner",owner,"at",today.atTime(12,0).atZone(ZoneId.of("Asia/Almaty")).toOffsetDateTime()));
  jdbc.update("insert into leads(id,branch_id,primary_contact_name,status,assigned_admin_id,next_action_at,work_priority,created_at) values(:id,:b,'Чужой администратор','NEW',:owner,:at,'NORMAL',:created)",Map.of("id",second,"b",owned,"owner",other,"at",today.atTime(10,0).atZone(ZoneId.of("Asia/Almaty")).toOffsetDateTime(),"created",today.atStartOfDay()));
  var q=query("","");var single=new CompanyQuery(q.branchIds(),"","",q.dateFrom(),q.dateTo(),0,1,true,"").withOwner(owner);
  var reader=new CompanyLeadOperationsReader(operations,projections,jdbc);var result=reader.tasks(single);
  assertEquals(1,result.totalElements());assertEquals(lead,result.content().getFirst().id());assertEquals(List.of(lead),reader.assignedLeadIds(owner,Set.of(owned)));assertTrue(reader.assignedLeadIds(owner,Set.of(foreign)).isEmpty());
 }

 @Test void debtTotalsAndPromisesUseAllMatchingRowsAndSeparateCurrencies() {
  var reader=new CompanyPaymentReader(projections);var totals=reader.debtMetrics(query("",""));assertEquals(1,totals.size());assertEquals("KZT",totals.getFirst().currency());assertEquals(10000,totals.getFirst().amount().intValue());assertTrue(reader.debtMetrics(query("Неизвестный"," ".trim())).isEmpty());
  var today=LocalDate.now(ZoneId.of("Asia/Almaty"));jdbc.update("insert into contract_follow_ups(contract_id,outcome,promised_payment_date,version) values(:id,'PAYMENT_PROMISED',:today,1)",Map.of("id",contract,"today",today));
  var follow=new ContractFollowUpService(jdbc,operations,new CompanyContractBalanceReader(jdbc),org.mockito.Mockito.mock(kz.edu.soccerhub.common.port.AdminPort.class));assertEquals(Map.of("promisedToday",1L,"endingSoon",1L),follow.summary(query("","")));
  jdbc.update("insert into payments values(:id,:c,:b,:p,10000,'KZT','PAID',:at,null)",Map.of("id",UUID.randomUUID(),"c",contract,"b",owned,"p",player,"at",LocalDateTime.now()));assertTrue(reader.debtMetrics(query("","")).isEmpty());assertEquals(0L,follow.summary(query("","")).get("promisedToday"));assertEquals(1L,follow.summary(query("","")).get("endingSoon"));
 }

}
