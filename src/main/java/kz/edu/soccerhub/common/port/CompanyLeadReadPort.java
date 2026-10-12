package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.List;
public interface CompanyLeadReadPort {
 CompanyPage page(CompanyQuery query);
 List<CompanyMetric> metrics(CompanyQuery query);
}
