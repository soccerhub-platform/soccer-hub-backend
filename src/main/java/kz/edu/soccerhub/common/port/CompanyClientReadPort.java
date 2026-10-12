package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.List;
public interface CompanyClientReadPort {
 CompanyPage page(CompanyQuery query, boolean contracts);
 List<CompanyMetric> metrics(CompanyQuery query);
}
