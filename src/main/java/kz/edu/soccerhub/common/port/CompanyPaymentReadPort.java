package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.List;
public interface CompanyPaymentReadPort {
 CompanyPage page(CompanyQuery query);
 CompanyPage debts(CompanyQuery query);
 List<CompanyMetric> debtMetrics(CompanyQuery q);
 List<CompanyMetric> metrics(CompanyQuery query);
}
