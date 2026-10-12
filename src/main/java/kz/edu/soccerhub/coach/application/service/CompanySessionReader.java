package kz.edu.soccerhub.coach.application.service;
import kz.edu.soccerhub.common.dto.trial.TrialSessionContext;
import kz.edu.soccerhub.common.port.CompanySessionReadPort;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.time.*;
@Service @RequiredArgsConstructor
public class CompanySessionReader implements CompanySessionReadPort {
 private final NamedParameterJdbcTemplate jdbc;
 public List<kz.edu.soccerhub.common.dto.company.CompanyMetric> attendanceMetrics(Map<UUID,UUID> groupBranches,kz.edu.soccerhub.common.dto.company.CompanyQuery q) {
  if(groupBranches.isEmpty())return List.of();
  var p=new MapSqlParameterSource("groups",groupBranches.keySet()).addValue("from",q.dateFrom()).addValue("to",q.dateTo());
  String filter=q.filterDates()?" and s.session_date between :from and :to":"";
  return jdbc.query("select s.group_id,a.status,count(*) n from training_session_attendance a join training_sessions s on s.id=a.session_id where s.group_id in (:groups) and s.status<>'CANCELLED'"+filter+" group by s.group_id,a.status",p,(rs,n)->new kz.edu.soccerhub.common.dto.company.CompanyMetric(groupBranches.get(rs.getObject("group_id",UUID.class)),"attendance:"+rs.getString("status"),rs.getLong("n"),null,null));
 }
 public Map<UUID,TrialSessionContext> sessions(Set<UUID> groups,LocalDate from,LocalDate to) {
  if(groups.isEmpty())return Map.of();
  var p=new MapSqlParameterSource("groups",groups).addValue("from",from).addValue("to",to);
  Map<UUID,TrialSessionContext> map=new HashMap<>();
  String filter=from==null?"":" and session_date between :from and :to";
  jdbc.query("select * from training_sessions where group_id in (:groups)"+filter,p,rs->{while(rs.next()) {var id=rs.getObject("id",UUID.class);map.put(id,new TrialSessionContext(id,rs.getObject("group_id",UUID.class),rs.getObject("coach_id",UUID.class),rs.getObject("location_id",UUID.class),rs.getObject("scheduled_start_at",LocalDateTime.class),rs.getObject("scheduled_end_at",LocalDateTime.class),TrainingSessionStatus.valueOf(rs.getString("status"))));}return map;});return map;
 }
}
