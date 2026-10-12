package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
@Builder(toBuilder=true)
public record OperationsRow(UUID id,UUID branchId,String kind,String name,String contact,String phone,String status,UUID ownerId,String ownerName,String nextAction,Instant dueAt,String priority,String reference,Integer occupied,Integer capacity,LocalDateTime startsAt,String coachName,BigDecimal amount,String currency) {}
