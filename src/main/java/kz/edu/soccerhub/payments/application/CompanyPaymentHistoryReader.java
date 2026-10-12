package kz.edu.soccerhub.payments.application;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.port.CompanyPaymentHistoryPort;
import kz.edu.soccerhub.common.application.CompanyProjectionReader;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.time.*;
@Service @RequiredArgsConstructor
public class CompanyPaymentHistoryReader implements CompanyPaymentHistoryPort {
 private final NamedParameterJdbcTemplate jdbc;
 private final CompanyProjectionReader params;
 public List<CompanyEntry> payments(Set<UUID> ids) {
  if(ids.isEmpty())return List.of();
  return jdbc.query("select * from payments where contract_id in (:ids) order by paid_at desc,id limit 100",new MapSqlParameterSource("ids",ids),(r,n)->CompanyEntry.builder().id(r.getObject("id",UUID.class)).branchId(r.getObject("branch_id",UUID.class)).reference(r.getObject("contract_id",UUID.class).toString()).status(r.getString("status")).amount(r.getBigDecimal("amount")).currency(r.getString("currency")).eventDate(r.getObject("paid_at",LocalDateTime.class)).notes(r.getString("comment")).build());
 }
 public List<DebtSnapshot> debtTrend(CompanyQuery q) {
  if(q.branchIds().isEmpty())return List.of();
  return jdbc.query("select * from payment_debt_snapshots where branch_id in (:ids) and (:filterDates=false or snapshot_date between cast(:from as date) and cast(:until as date)-1) order by snapshot_date,branch_id,currency",params.params(q),(r,n)->new DebtSnapshot(r.getObject("branch_id",UUID.class),r.getObject("snapshot_date",LocalDate.class),r.getString("currency"),r.getLong("contract_count"),r.getBigDecimal("amount")));
 }
 public void captureDebt() {
  jdbc.update("""
   insert into payment_debt_snapshots(branch_id,snapshot_date,currency,contract_count,amount)
   select cl.branch_id,:today,c.currency,count(*) filter(where c.amount>coalesce(paid.amount,0)),sum(greatest(c.amount-coalesce(paid.amount,0),0))
   from contracts c join client_profiles cl on cl.id=c.client_id
   left join (select contract_id,sum(amount) amount from payments where status='PAID' group by contract_id) paid on paid.contract_id=c.id
   where c.status not in ('DRAFT','CANCELLED') and c.start_date<=:today group by cl.branch_id,c.currency
   on conflict(branch_id,snapshot_date,currency) do nothing
   """,new MapSqlParameterSource("today",LocalDate.now(ZoneId.of("Asia/Almaty"))));
 }
}
