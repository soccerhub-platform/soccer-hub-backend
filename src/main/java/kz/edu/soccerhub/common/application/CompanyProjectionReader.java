package kz.edu.soccerhub.common.application;
import kz.edu.soccerhub.common.dto.company.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.sql.*;
import java.time.*;
import java.util.*;
@Component
@RequiredArgsConstructor
public class CompanyProjectionReader {
 private final NamedParameterJdbcTemplate jdbc;
 public MapSqlParameterSource params(CompanyQuery q) {
  String search = q.search().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
  return new MapSqlParameterSource().addValue("ids", q.branchIds()).addValue("search", "%"+search+"%")
   .addValue("attention",q.attention()).addValue("filterDates",q.filterDates()).addValue("status",q.status()).addValue("from",q.dateFrom().atStartOfDay())
   .addValue("until",q.dateTo().plusDays(1).atStartOfDay()).addValue("today",LocalDate.now(ZoneId.of("Asia/Almaty")))
   .addValue("soon",LocalDate.now(ZoneId.of("Asia/Almaty")).plusDays(14))
   .addValue("dayFrom",q.dateFrom().atStartOfDay(ZoneId.of("Asia/Almaty")).toOffsetDateTime()).addValue("dayUntil",q.dateTo().plusDays(1).atStartOfDay(ZoneId.of("Asia/Almaty")).toOffsetDateTime())
   .addValue("now",OffsetDateTime.now(ZoneOffset.UTC)).addValue("limit",q.size()).addValue("offset",(long)q.page()*q.size());
 }
 public CompanyPage page(CompanyQuery q,String projection,String source,String filter,String order) {
  if(q.branchIds().isEmpty()) return new CompanyPage(List.of(),0,0,q.page(),q.size());
  var p=params(q);
  long total=Objects.requireNonNull(jdbc.queryForObject("select count(*) "+source+" where "+filter,p,Long.class));
  var rows=jdbc.query("select "+projection+" "+source+" where "+filter+" order by "+order+" limit :limit offset :offset",p,(rs,n)->
   CompanyEntry.builder().id(rs.getObject("id",UUID.class)).branchId(rs.getObject("branch_id",UUID.class))
    .reference(rs.getString("reference")).name(rs.getString("name")).contact(rs.getString("contact"))
    .phone(rs.getString("phone")).email(rs.getString("email")).status(rs.getString("status"))
    .birthDate(rs.getObject("birth_date",LocalDate.class)).startDate(rs.getObject("start_date",LocalDate.class))
    .endDate(rs.getObject("end_date",LocalDate.class)).amount(rs.getBigDecimal("amount"))
    .currency(rs.getString("currency")).eventDate(rs.getObject("event_date",LocalDateTime.class)).notes(rs.getString("notes")).build());
  return new CompanyPage(rows,total,(int)((total+q.size()-1)/q.size()),q.page(),q.size());
 }
 public List<CompanyMetric> metrics(CompanyQuery q,String sql) {
  if(q.branchIds().isEmpty()) return List.of();
  return jdbc.query(sql,params(q),(rs,n)->new CompanyMetric(rs.getObject("branch_id",UUID.class),rs.getString("key"),rs.getLong("count"),rs.getString("currency"),rs.getBigDecimal("amount")));
 }
}
