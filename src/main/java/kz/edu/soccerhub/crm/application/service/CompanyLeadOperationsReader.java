package kz.edu.soccerhub.crm.application.service;
import kz.edu.soccerhub.common.application.*;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.port.CompanyLeadOperationsPort;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class CompanyLeadOperationsReader implements CompanyLeadOperationsPort {
 private final OperationsProjectionReader operations;
 private final CompanyProjectionReader metrics;
 private final NamedParameterJdbcTemplate jdbc;
 public List<UUID> assignedLeadIds(UUID owner,Set<UUID> branches) {if(branches.isEmpty())return List.of();return jdbc.queryForList("select id from leads where branch_id in (:ids) and assigned_admin_id=:owner",new MapSqlParameterSource("ids",branches).addValue("owner",owner),UUID.class);}
 public OperationsPage tasks(CompanyQuery q) {
  return operations.page(q,"""
   select l.id,l.branch_id,'LEAD' as kind,l.primary_contact_name as name,l.primary_contact_name as contact,l.primary_contact_phone as phone,l.status,l.assigned_admin_id as owner_id,coalesce(l.next_action,case when l.last_contact_at is null then 'Первый контакт' else 'Назначить следующий шаг' end) as next_action,l.next_action_at as due_at,l.work_priority as priority,null::text as reference,null::integer as occupied,null::integer as capacity,null::timestamp as starts_at,null::text as coach_name,null::numeric as amount,null::text as currency
   from leads l where l.branch_id in (:ids) and l.status not in ('CONVERTED','LOST')
   and (:status='' or (:status='OVERDUE' and l.next_action_at<:now) or (:status='TODAY' and l.next_action_at>=:dayFrom and l.next_action_at<:dayUntil) or (:status='UNASSIGNED' and l.assigned_admin_id is null) or (:status='NO_CONTACT' and l.last_contact_at is null) or (:status='NO_ACTION' and l.next_action is null))
   and lower(concat_ws(' ',l.primary_contact_name,l.primary_contact_phone,l.next_action)) like :search escape '!'
   ""","due_at asc nulls last,case priority when 'URGENT' then 0 when 'HIGH' then 1 else 2 end,id");
 }
 public Map<String,String> lossReasonNames() {
  Map<String,String> names=new HashMap<>();names.put("UNKNOWN","Причина не указана");
  jdbc.query("select code,name from lead_loss_reasons",new MapSqlParameterSource(),r->{while(r.next())names.put(r.getString("code"),r.getString("name"));return names;});return names;
 }
 public List<CompanyMetric> report(CompanyQuery q) {
  return metrics.metrics(q,"""
   select branch_id,concat('stage:',status) as key,count(*) as count,null::text as currency,null::numeric as amount from leads where branch_id in (:ids) and (:filterDates=false or (created_at>=:from and created_at<:until)) group by branch_id,status
   union all
   select branch_id,concat('loss:',coalesce(lost_reason_code,'UNKNOWN')),count(*),null::text,null::numeric from leads where branch_id in (:ids) and lost_at is not null and (:filterDates=false or (lost_at>=:from and lost_at<:until)) group by branch_id,lost_reason_code
   union all
   select l.branch_id,'responseMinutes',count(*),null::text,sum(extract(epoch from (a.first_contact-l.created_at))/60)::numeric
   from leads l join (select lead_id,min(created_at) first_contact from lead_activities where activity_type='WORK_UPDATED' and details like 'Контакт%' group by lead_id) a on a.lead_id=l.id
   where l.branch_id in (:ids) and (:filterDates=false or (l.created_at>=:from and l.created_at<:until)) and a.first_contact>=l.created_at group by l.branch_id
   """);
 }
 public List<UUID> studentLeads(UUID student,Set<UUID> branches) {
  if(branches.isEmpty())return List.of();
  return jdbc.queryForList("select distinct l.id from leads l join lead_participants p on p.lead_id=l.id where p.player_id=:student and l.branch_id in (:ids)",new MapSqlParameterSource("student",student).addValue("ids",branches),UUID.class);
 }
}
