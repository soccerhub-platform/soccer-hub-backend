package kz.edu.soccerhub.crm.application.service;
import kz.edu.soccerhub.common.application.CompanyProjectionReader;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.port.CompanyLeadReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.LocalDate;
import org.springframework.jdbc.core.namedparam.*;
@Service
@RequiredArgsConstructor
public class CompanyLeadReader implements CompanyLeadReadPort {
 private final CompanyProjectionReader reader;
 private final NamedParameterJdbcTemplate jdbc;
 private final kz.edu.soccerhub.common.port.AdminPort admins;
 public CompanyPage page(CompanyQuery q) {
  var page = reader.page(q,
   "l.id,l.branch_id,null::text reference,l.primary_contact_name name,l.primary_contact_name contact,l.primary_contact_phone phone,l.primary_contact_email email,l.status,null::date birth_date,null::date start_date,null::date end_date,null::numeric amount,null::text currency,l.created_at event_date,l.comment notes",
   "from leads l",
   "l.branch_id in (:ids) and (:filterDates=false or (l.created_at>=:from and l.created_at<:until)) and (:status='' or l.status=:status) and (:attention='' or (:attention='UNASSIGNED' and l.status not in ('CONVERTED','LOST') and l.assigned_admin_id is null) or (:attention='NO_CONTACT' and l.status not in ('CONVERTED','LOST') and l.last_contact_at is null) or (:attention='NO_ACTION' and l.status not in ('CONVERTED','LOST') and l.next_action is null) or (:attention='OVERDUE' and l.status not in ('CONVERTED','LOST') and l.next_action_at<:now)) and lower(concat_ws(' ',l.primary_contact_name,l.primary_contact_phone,l.primary_contact_email)) like :search escape '!'",
   kz.edu.soccerhub.common.application.CompanySort.order(q.sort(),"l.created_at desc,l.id",java.util.Map.of("NEWEST","l.created_at desc,l.id","OLDEST","l.created_at asc,l.id","NAME","lower(l.primary_contact_name),l.id","DUE","l.next_action_at asc nulls last,l.id")));
  if(page.content().isEmpty()) return page;
  Map<UUID,List<CompanyParticipant>> participants = new HashMap<>();
  jdbc.query("select lead_id,full_name,birth_date from lead_participants where lead_id in (:ids) order by id",
   new MapSqlParameterSource("ids",page.content().stream().map(CompanyEntry::id).toList()),rs -> {
    while(rs.next()) participants.computeIfAbsent(rs.getObject("lead_id",UUID.class),id->new ArrayList<>())
     .add(new CompanyParticipant(rs.getString("full_name"),rs.getObject("birth_date",LocalDate.class)));
    return participants;
   });
  Map<UUID,CompanyEntry> work=new HashMap<>();
  jdbc.query("select id,assigned_admin_id,next_action,next_action_at,last_contact_at,work_priority from leads where id in (:ids)",
   new MapSqlParameterSource("ids",page.content().stream().map(CompanyEntry::id).toList()),rs->{while(rs.next()) {var id=rs.getObject("id",UUID.class);var owner=rs.getObject("assigned_admin_id",UUID.class);work.put(id,CompanyEntry.builder().ownerId(owner).nextAction(rs.getString("next_action")).dueAt(rs.getTimestamp("next_action_at")==null?null:rs.getTimestamp("next_action_at").toInstant()).lastContactAt(rs.getTimestamp("last_contact_at")==null?null:rs.getTimestamp("last_contact_at").toInstant()).priority(rs.getString("work_priority")).build());}return work;});
  Map<UUID,String> names=new HashMap<>();
  for(var w:work.values())if(w.ownerId()!=null&&!names.containsKey(w.ownerId()))names.put(w.ownerId(),admins.findById(w.ownerId()).map(a->a.firstName()+" "+a.lastName()).orElse("Администратор недоступен"));
  return new CompanyPage(page.content().stream().map(row->{var w=work.get(row.id());return row.toBuilder().participants(participants.getOrDefault(row.id(),List.of())).ownerId(w.ownerId()).ownerName(w.ownerId()==null?null:names.get(w.ownerId())).nextAction(w.nextAction()).dueAt(w.dueAt()).lastContactAt(w.lastContactAt()).priority(w.priority()).build();}).toList(),
   page.totalElements(),page.totalPages(),page.number(),page.size());
 }
 public List<CompanyMetric> metrics(CompanyQuery q) {
  return reader.metrics(q,"""
   select branch_id,'newLeads' key,count(*) count,null::text currency,null::numeric amount from leads
   where branch_id in (:ids) and (:filterDates=false or (created_at>=:from and created_at<:until)) group by branch_id
   union all
   select branch_id,'convertedLeads',count(*),null::text,null::numeric from leads
   where branch_id in (:ids) and (:filterDates=false or (created_at>=:from and created_at<:until)) and status='CONVERTED' group by branch_id
   union all
   select branch_id,'unassignedLeads',count(*),null::text,null::numeric from leads
   where branch_id in (:ids) and status not in ('CONVERTED','LOST') and assigned_admin_id is null group by branch_id
   union all
   select branch_id,'overdueTasks',count(*),null::text,null::numeric from leads
   where branch_id in (:ids) and status not in ('CONVERTED','LOST') and next_action_at<:now group by branch_id
   """);
 }
}
