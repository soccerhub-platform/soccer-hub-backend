package kz.edu.soccerhub.client.application;
import kz.edu.soccerhub.common.application.CompanyProjectionReader;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.port.CompanyClientReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CompanyClientReader implements CompanyClientReadPort {
 private final CompanyProjectionReader reader;
 // Contract lifecycle is calculated without mutating records during a company report.
 private static final String STATUS = "case when c.status in ('CANCELLED','DRAFT') then c.status when c.start_date > :today then 'UPCOMING' when c.end_date < :today then 'EXPIRED' else 'ACTIVE' end";
 public CompanyPage page(CompanyQuery q,boolean contracts) {
  if(!contracts) return reader.page(q,
   "p.id, cl.branch_id, null::text reference, concat_ws(' ',p.first_name,p.last_name) name, concat_ws(' ',cl.first_name,cl.last_name) contact, cl.phone, cl.email, cl.status, p.birth_date, null::date start_date, null::date end_date, null::numeric amount, null::text currency, p.created_at event_date, null::text notes",
   "from players p join client_profiles cl on cl.id=p.parent_id",
   "cl.branch_id in (:ids) and (:status='' or cl.status=:status) and lower(concat_ws(' ',p.first_name,p.last_name,cl.first_name,cl.last_name,cl.phone,cl.email)) like :search escape '!'",
   kz.edu.soccerhub.common.application.CompanySort.order(q.sort(),"lower(p.first_name),lower(p.last_name),p.id",java.util.Map.of("NAME","lower(p.first_name),lower(p.last_name),p.id","NEWEST","p.created_at desc,p.id","OLDEST","p.created_at asc,p.id","BIRTH","p.birth_date asc nulls last,p.id")));
  return reader.page(q,
   "c.id, cl.branch_id, c.contract_number reference, concat_ws(' ',p.first_name,p.last_name) name, concat_ws(' ',cl.first_name,cl.last_name) contact, cl.phone, cl.email, "+STATUS+" status, p.birth_date, c.start_date, c.end_date, c.amount, c.currency, c.created_at event_date, c.notes",
   "from contracts c join client_profiles cl on cl.id=c.client_id join players p on p.id=c.player_id",
   "cl.branch_id in (:ids) and (:status='' or ("+STATUS+")=:status) and (:attention='' or (:attention='ENDING' and c.status not in ('CANCELLED','DRAFT') and c.start_date<=:today and c.end_date between :today and :soon)) and lower(concat_ws(' ',c.contract_number,p.first_name,p.last_name,cl.first_name,cl.last_name,cl.phone)) like :search escape '!'",
   kz.edu.soccerhub.common.application.CompanySort.order(q.sort(),"c.created_at desc,c.id",java.util.Map.of("NEWEST","c.created_at desc,c.id","OLDEST","c.created_at asc,c.id","NAME","lower(p.first_name),lower(p.last_name),c.id","ENDING","c.end_date asc nulls last,c.id")));
 }
 public List<CompanyMetric> metrics(CompanyQuery q) {
  return reader.metrics(q,"""
   select cl.branch_id, 'students' key, count(*) count, null::text currency, null::numeric amount
   from players p join client_profiles cl on cl.id=p.parent_id where cl.branch_id in (:ids) group by cl.branch_id
   union all
   select cl.branch_id, 'activeContracts', count(*), null::text, null::numeric
   from contracts c join client_profiles cl on cl.id=c.client_id
   where cl.branch_id in (:ids) and c.status not in ('CANCELLED','DRAFT') and c.start_date<=:today and (c.end_date is null or c.end_date>=:today) group by cl.branch_id
   union all
   select cl.branch_id, 'endingContracts', count(*), null::text, null::numeric
   from contracts c join client_profiles cl on cl.id=c.client_id
   where cl.branch_id in (:ids) and c.status not in ('CANCELLED','DRAFT') and c.start_date<=:today and c.end_date between :today and :soon group by cl.branch_id
   """);
 }
}
