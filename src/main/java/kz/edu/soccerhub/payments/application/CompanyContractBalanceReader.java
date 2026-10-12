package kz.edu.soccerhub.payments.application;
import kz.edu.soccerhub.common.port.CompanyContractBalancePort;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;
@Service @RequiredArgsConstructor
public class CompanyContractBalanceReader implements CompanyContractBalancePort {
 private final NamedParameterJdbcTemplate jdbc;
 public BigDecimal outstandingContract(UUID id) {
  return jdbc.queryForObject("select greatest(c.amount-coalesce((select sum(p.amount) from payments p where p.contract_id=c.id and p.status='PAID'),0),0) from contracts c where c.id=:id",Map.of("id",id),BigDecimal.class);
 }
 public Map<UUID,BigDecimal> debtContracts(Set<UUID> branches) {
  if(branches.isEmpty())return Map.of();var p=new MapSqlParameterSource("ids",branches).addValue("today",LocalDate.now(ZoneId.of("Asia/Almaty")));Map<UUID,BigDecimal> result=new HashMap<>();
  jdbc.query("""
   select c.id,greatest(c.amount-coalesce(paid.amount,0),0) amount from contracts c join client_profiles cl on cl.id=c.client_id
   left join (select contract_id,sum(amount) amount from payments where status='PAID' group by contract_id) paid on paid.contract_id=c.id
   where cl.branch_id in (:ids) and c.status not in ('DRAFT','CANCELLED') and c.start_date<=:today and c.amount>coalesce(paid.amount,0)
   """,p,r->{while(r.next())result.put(r.getObject("id",UUID.class),r.getBigDecimal("amount"));return result;});return result;
 }
}
