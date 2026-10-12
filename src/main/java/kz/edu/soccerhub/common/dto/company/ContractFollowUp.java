package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.time.*;
import java.util.*;
@Builder
public record ContractFollowUp(UUID contractId,UUID ownerId,String nextAction,Instant dueAt,String outcome,LocalDate promisedPaymentDate,Instant lastContactAt,long version,List<TimelineEvent> history) {}
