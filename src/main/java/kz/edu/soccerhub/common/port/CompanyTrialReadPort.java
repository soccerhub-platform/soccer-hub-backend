package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.*;
public interface CompanyTrialReadPort {
 Map<String,Long> summary(Set<UUID> sessionIds,CompanyQuery q);
 TrialCompanyPage page(Set<UUID> sessionIds,CompanyQuery q);
 OperationsPage tasks(Set<UUID> sessionIds,CompanyQuery q);
}
