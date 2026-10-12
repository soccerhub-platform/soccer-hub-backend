package kz.edu.soccerhub.payments.application;
import kz.edu.soccerhub.common.application.CompanyProjectionReader;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.port.CompanyPaymentReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CompanyPaymentReader implements CompanyPaymentReadPort {
 private final CompanyProjectionReader reader;
 public CompanyPage page(CompanyQuery q) {
  return reader.page(q,
   "pay.id, pay.branch_id, c.contract_number reference, concat_ws(' ',p.first_name,p.last_name) name, concat_ws(' ',cl.first_name,cl.last_name) contact, cl.phone, cl.email, pay.status, null::date birth_date, null::date start_date, null::date end_date, pay.amount, pay.currency, pay.paid_at event_date, concat_ws(' · ',pay.method,pay.comment,pay.cancel_reason,pay.cancel_comment) notes",
   "from payments pay join contracts c on c.id=pay.contract_id join client_profiles cl on cl.id=pay.client_id left join players p on p.id=pay.player_id",
   "pay.branch_id in (:ids) and (:status='' or pay.status=:status) and (:filterDates=false or (pay.paid_at>=:from and pay.paid_at<:until)) and lower(concat_ws(' ',c.contract_number,p.first_name,p.last_name,cl.first_name,cl.last_name,cl.phone,pay.external_reference)) like :search escape '!'",
   kz.edu.soccerhub.common.application.CompanySort.order(q.sort(),"pay.paid_at desc,pay.id",java.util.Map.of("NEWEST","pay.paid_at desc,pay.id","OLDEST","pay.paid_at asc,pay.id","NAME","lower(p.first_name),lower(p.last_name),pay.id","AMOUNT","pay.currency,pay.amount desc,pay.id")));
 }
 public CompanyPage debts(CompanyQuery q) {
  return reader.page(q,
   "c.id,cl.branch_id,c.contract_number reference,concat_ws(' ',p.first_name,p.last_name) name,concat_ws(' ',cl.first_name,cl.last_name) contact,cl.phone,cl.email,case when c.end_date<:today then 'EXPIRED' else 'ACTIVE' end status,p.birth_date,c.start_date,c.end_date,greatest(c.amount-coalesce(paid.amount,0),0) amount,c.currency,c.created_at event_date,c.notes",
   "from contracts c join client_profiles cl on cl.id=c.client_id join players p on p.id=c.player_id left join (select contract_id,sum(amount) amount from payments where status='PAID' group by contract_id) paid on paid.contract_id=c.id",
   "cl.branch_id in (:ids) and c.status not in ('DRAFT','CANCELLED') and c.start_date<=:today and c.amount>coalesce(paid.amount,0) and (:status='' or (:status='EXPIRED' and c.end_date<:today) or (:status='ACTIVE' and (c.end_date is null or c.end_date>=:today))) and lower(concat_ws(' ',c.contract_number,p.first_name,p.last_name,cl.first_name,cl.last_name,cl.phone)) like :search escape '!'",
   kz.edu.soccerhub.common.application.CompanySort.order(q.sort(),"c.end_date asc nulls last,c.id",java.util.Map.of("ENDING","c.end_date asc nulls last,c.id","NAME","lower(p.first_name),lower(p.last_name),c.id","AMOUNT","c.currency,greatest(c.amount-coalesce(paid.amount,0),0) desc,c.id","NEWEST","c.created_at desc,c.id")));
 }
 public List<CompanyMetric> debtMetrics(CompanyQuery q) {
  return reader.metrics(q,"""
select cl.branch_id,'debt' key,count(*) count,c.currency,sum(greatest(c.amount-coalesce(paid.amount,0),0)) amount
from contracts c join client_profiles cl on cl.id=c.client_id join players p on p.id=c.player_id
left join (select contract_id,sum(amount) amount from payments where status='PAID' group by contract_id) paid on paid.contract_id=c.id
where cl.branch_id in (:ids) and c.status not in ('DRAFT','CANCELLED') and c.start_date<=:today and c.amount>coalesce(paid.amount,0)
and (:status='' or (:status='EXPIRED' and c.end_date<:today) or (:status='ACTIVE' and (c.end_date is null or c.end_date>=:today)))
and lower(concat_ws(' ',c.contract_number,p.first_name,p.last_name,cl.first_name,cl.last_name,cl.phone)) like :search escape '!'
group by cl.branch_id,c.currency
""");
 }
 public List<CompanyMetric> metrics(CompanyQuery q) {
  return reader.metrics(q,"""
select branch_id,'receipts' key,count(*) count,currency,sum(amount) amount from payments where branch_id in (:ids) and status='PAID' and (:filterDates=false or (paid_at>=:from and paid_at<:until)) group by branch_id,currency
union all
select branch_id,'cancelledPayments',count(*),currency,null::numeric from payments
where branch_id in (:ids) and status='CANCELLED' and (:filterDates=false or (paid_at>=:from and paid_at<:until)) group by branch_id,currency
union all
select cl.branch_id,'debt' key,count(*) count,c.currency,sum(greatest(c.amount-coalesce(paid.amount,0),0)) amount
from contracts c join client_profiles cl on cl.id=c.client_id
left join (select contract_id,sum(amount) amount from payments where status='PAID' group by contract_id) paid on paid.contract_id=c.id
where cl.branch_id in (:ids) and c.status not in ('DRAFT','CANCELLED') and c.start_date<=:today and c.amount>coalesce(paid.amount,0)
group by cl.branch_id,c.currency
""");
 }
}
