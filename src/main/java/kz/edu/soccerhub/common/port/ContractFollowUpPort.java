package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.UUID;
public interface ContractFollowUpPort {
 ContractFollowUp get(UUID contractId);
 void update(UUID id,UUID actor,FollowUpCommand c);
 void lockForPayment(UUID id);
 void paymentRecorded(UUID id,UUID actor);
 java.util.Map<String,Long> summary(CompanyQuery q);
 OperationsPage tasks(CompanyQuery q);
}
