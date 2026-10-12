package kz.edu.soccerhub.coach.application.service;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.company.CompanyQuery;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.junit.jupiter.api.*;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CoachHistoryAnalyticsReaderTest {
 NamedParameterJdbcTemplate jdbc;GroupPort groups;CoachHistoryAnalyticsReader reader;
 UUID coach=UUID.randomUUID(),other=UUID.randomUUID(),group=UUID.randomUUID(),foreignGroup=UUID.randomUUID(),branch=UUID.randomUUID();
 LocalDate today=LocalDate.now(ZoneId.of("Asia/Almaty"));
 @BeforeEach void setup(){
  jdbc=new NamedParameterJdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));groups=mock(GroupPort.class);reader=new CoachHistoryAnalyticsReader(jdbc,groups);
  jdbc.getJdbcTemplate().execute("create table training_sessions(id uuid primary key,group_id uuid,coach_id uuid,session_date date,scheduled_end_at timestamp,status varchar,report_done boolean)");
  jdbc.getJdbcTemplate().execute("create table training_session_attendance(id uuid primary key,session_id uuid,status varchar)");
  when(groups.getGroupsByIds(any())).thenReturn(List.of(GroupDto.builder().groupId(group).name("Младшая группа").build()));
  session(coach,group,today.minusDays(2),"COMPLETED",true,"PRESENT");
  session(coach,group,today.minusYears(3),"COMPLETED",true,"ABSENT");
  session(coach,group,today.minusDays(1),"IN_PROGRESS",false,"LATE");
  session(coach,group,today.minusDays(1),"CANCELLED",false,"ABSENT");
  session(coach,group,today.plusDays(1),"PLANNED",false,"PRESENT");
  session(other,foreignGroup,today.minusDays(1),"COMPLETED",true,"PRESENT");
 }
 void session(UUID owner,UUID groupId,LocalDate date,String status,boolean done,String attendance){
  var id=UUID.randomUUID();jdbc.update("insert into training_sessions values(:id,:g,:c,:day,:end,:status,:done)",Map.of("id",id,"g",groupId,"c",owner,"day",date,"end",date.atTime(12,0),"status",status,"done",done));
  jdbc.update("insert into training_session_attendance values(:id,:s,:status)",Map.of("id",UUID.randomUUID(),"s",id,"status",attendance));
 }
 @Test void allTimeCountsOnlyOwnPastSessionsAndSavedAttendance(){
  var data=reader.read(coach,null,null,null,"","");assertEquals(Map.of("PRESENT",1L,"ABSENT",1L,"LATE",1L),data.attendance());assertEquals(4,data.workload().getFirst().value());verify(groups).getGroupsByIds(Set.of(group));
 }
 @Test void periodGroupReportAndStatusFiltersApplyToWholeAnalytics(){
  var data=reader.read(coach,today.minusDays(7),today,group,"COMPLETED","DONE");assertEquals(Map.of("PRESENT",1L),data.attendance());assertEquals(1,data.workload().getFirst().value());
  data=reader.read(coach,null,null,null,"OVERDUE","PENDING");assertEquals(Map.of("LATE",1L),data.attendance());assertEquals(1,data.workload().getFirst().value());
 }
 @Test void branchAttendanceProjectionUsesOnlyScopedGroupsAndPeriod(){
  var projection=new CompanySessionReader(jdbc);var q=new CompanyQuery(Set.of(branch),"","",today.minusDays(7),today,0,25,true,"");
  var metrics=projection.attendanceMetrics(Map.of(group,branch),q);assertEquals(2,metrics.stream().mapToLong(m->m.count()).sum());assertTrue(metrics.stream().allMatch(m->m.branchId().equals(branch)));assertTrue(projection.attendanceMetrics(Map.of(),q).isEmpty());
 }
}
