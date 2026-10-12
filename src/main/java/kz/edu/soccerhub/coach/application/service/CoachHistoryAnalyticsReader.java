package kz.edu.soccerhub.coach.application.service;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.coach.application.dto.session.CoachHistoryResponse.Workload;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.time.*;
@Service @RequiredArgsConstructor
public class CoachHistoryAnalyticsReader {
 private final NamedParameterJdbcTemplate jdbc;
 private final GroupPort groups;
 public record Analytics(Map<String,Long> attendance,List<Workload> workload) {}
 public Analytics read(UUID coach,LocalDate from,LocalDate to,UUID group,String status,String report) {
  var now=LocalDateTime.now(ZoneId.of("Asia/Almaty"));
  var p=new MapSqlParameterSource("coach",coach).addValue("today",now.toLocalDate());
  String where="s.coach_id=:coach and s.session_date<=:today";
  if(from!=null){where+=" and s.session_date between :from and :to";p.addValue("from",from).addValue("to",to);}
  if(group!=null){where+=" and s.group_id=:group";p.addValue("group",group);}
  if("OVERDUE".equals(status)){where+=" and s.status in ('PLANNED','IN_PROGRESS') and s.scheduled_end_at<:now";p.addValue("now",now);}
  else if(!status.isEmpty()){where+=" and s.status=:status";p.addValue("status",status);}
  if(!report.isEmpty()){where+=" and s.report_done=:done";p.addValue("done",report.equals("DONE"));}
  var attendance=new LinkedHashMap<String,Long>();
  jdbc.query("select a.status,count(*) n from training_session_attendance a join training_sessions s on s.id=a.session_id where "+where+" and s.status<>'CANCELLED' group by a.status",p,r->{while(r.next())attendance.put(r.getString("status"),r.getLong("n"));return null;});
  var counts=new LinkedHashMap<UUID,Long>();
  jdbc.query("select s.group_id,count(*) n from training_sessions s where "+where+" group by s.group_id order by n desc,s.group_id",p,r->{while(r.next())counts.put(r.getObject("group_id",UUID.class),r.getLong("n"));return null;});
  var names=new HashMap<UUID,String>();
  if(!counts.isEmpty())groups.getGroupsByIds(counts.keySet()).forEach(g->names.put(g.groupId(),g.name()));
  return new Analytics(attendance,counts.entrySet().stream().map(e->new Workload(names.getOrDefault(e.getKey(),"Группа недоступна"),e.getValue())).toList());
 }
}
