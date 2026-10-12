package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.*;
public interface CompanyOrganizationPort {
 OperationsPage groups(CompanyQuery query);
 Map<String,Long> summary(CompanyQuery query);
 Map<UUID,UUID> groupBranches(Set<UUID> branches);
}
