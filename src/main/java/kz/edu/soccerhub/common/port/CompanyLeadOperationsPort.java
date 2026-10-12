package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.*;
public interface CompanyLeadOperationsPort {
 List<UUID> assignedLeadIds(UUID owner, Set<UUID> branches);
 OperationsPage tasks(CompanyQuery query);
 Map<String,String> lossReasonNames();
 List<CompanyMetric> report(CompanyQuery query);
 List<UUID> studentLeads(UUID studentId,Set<UUID> branches);
}
