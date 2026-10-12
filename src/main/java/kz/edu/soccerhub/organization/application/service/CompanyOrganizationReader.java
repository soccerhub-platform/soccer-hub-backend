package kz.edu.soccerhub.organization.application.service;
import kz.edu.soccerhub.common.application.*;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.port.CompanyOrganizationPort;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class CompanyOrganizationReader implements CompanyOrganizationPort {
 private final OperationsProjectionReader reader;
 private final NamedParameterJdbcTemplate jdbc;
 public OperationsPage groups(CompanyQuery q) {
  return reader.page(q,"""
   select g.id,g.branch_id,'GROUP' as kind,g.name,null::text as contact,null::text as phone,g.status,null::uuid as owner_id,null::text as next_action,null::timestamptz as due_at,'NORMAL' as priority,null::text as reference,coalesce(m.occupied,0)::integer as occupied,g.capacity,null::timestamp as starts_at,null::text as coach_name,null::numeric as amount,null::text as currency
   from groups g left join (select group_id,count(*) occupied from group_memberships where status in ('ACTIVE','UPCOMING') and joined_at<=:today and (left_at is null or left_at>=:today) group by group_id) m on m.group_id=g.id
   where g.branch_id in (:ids) and (:status='' or g.status=:status or (:status='FULL' and g.capacity is not null and coalesce(m.occupied,0)>=g.capacity) or (:status='AVAILABLE' and g.status='ACTIVE' and (g.capacity is null or coalesce(m.occupied,0)<g.capacity))) and lower(g.name) like :search escape '!'
   """,kz.edu.soccerhub.common.application.CompanySort.order(q.sort(),"name,id",java.util.Map.of("NAME","name,id","OCCUPIED","occupied desc,id","AVAILABLE","(capacity-occupied) desc nulls last,id","FULL","case when capacity>0 then occupied*1.0/capacity else null end desc nulls last,id")));
 }
 public Map<String,Long> summary(CompanyQuery q) {
  if(q.branchIds().isEmpty())return Map.of();
  var p=new kz.edu.soccerhub.common.application.CompanyProjectionReader(jdbc).params(q);
  return jdbc.queryForObject("""
   select count(*) total_groups,count(*) filter(where g.status='ACTIVE') total_active,
    count(*) filter(where g.status='ACTIVE' and g.capacity is not null and coalesce(m.n,0)>=g.capacity) total_full,
    coalesce(sum(greatest(g.capacity-coalesce(m.n,0),0)) filter(where g.status='ACTIVE' and g.capacity is not null),0) total_available
   from groups g left join (select group_id,count(*) n from group_memberships where status in ('ACTIVE','UPCOMING') and joined_at<=:today and (left_at is null or left_at>=:today) group by group_id) m on m.group_id=g.id where g.branch_id in (:ids)
   """,p,(r,n)->Map.of("groups",r.getLong("total_groups"),"active",r.getLong("total_active"),"full",r.getLong("total_full"),"available",r.getLong("total_available")));
 }
 public Map<UUID,UUID> groupBranches(Set<UUID> branches) {
  if(branches.isEmpty())return Map.of();
  Map<UUID,UUID> map=new HashMap<>();
  jdbc.query("select id,branch_id from groups where branch_id in (:ids)",new MapSqlParameterSource("ids",branches),rs->{while(rs.next())map.put(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class));return map;});return map;
 }
}
