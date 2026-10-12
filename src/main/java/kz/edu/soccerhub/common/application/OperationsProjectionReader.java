package kz.edu.soccerhub.common.application;
import kz.edu.soccerhub.common.dto.company.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.time.*;
@Component @RequiredArgsConstructor
public class OperationsProjectionReader {
 private final NamedParameterJdbcTemplate jdbc;
 private final CompanyProjectionReader params;
 public OperationsPage page(CompanyQuery q,String sql,String order) {return page(q,sql,order,Map.of());}
 public OperationsPage page(CompanyQuery q,String sql,String order,Map<String,?> extra) {
  if(q.branchIds().isEmpty())return new OperationsPage(List.of(),0,0,q.page(),q.size());
  var p=params.params(q);extra.forEach(p::addValue);
  p.addValue("owner",q.ownerId(),java.sql.Types.OTHER);
  String ownerFilter=q.ownerId()==null?"":" where op.owner_id=:owner";
  long count=Objects.requireNonNull(jdbc.queryForObject("select count(*) from ("+sql+") op"+ownerFilter,p,Long.class));
  var rows=jdbc.query("select * from ("+sql+") op"+ownerFilter+" order by "+order+" limit :limit offset :offset",p,(r,n)->OperationsRow.builder()
   .id(r.getObject("id",UUID.class)).branchId(r.getObject("branch_id",UUID.class)).kind(r.getString("kind")).name(r.getString("name"))
   .contact(r.getString("contact")).phone(r.getString("phone")).status(r.getString("status")).ownerId(r.getObject("owner_id",UUID.class))
   .nextAction(r.getString("next_action")).dueAt(r.getTimestamp("due_at")==null?null:r.getTimestamp("due_at").toInstant()).priority(r.getString("priority"))
   .reference(r.getString("reference")).occupied(r.getObject("occupied",Integer.class)).capacity(r.getObject("capacity",Integer.class))
   .startsAt(r.getObject("starts_at",LocalDateTime.class)).coachName(r.getString("coach_name")).amount(r.getBigDecimal("amount")).currency(r.getString("currency")).build());
  return new OperationsPage(rows,count,(int)((count+q.size()-1)/q.size()),q.page(),q.size());
 }
}
