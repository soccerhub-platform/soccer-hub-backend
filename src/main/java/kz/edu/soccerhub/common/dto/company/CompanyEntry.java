package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
@Builder(toBuilder = true)
public record CompanyEntry(UUID id, UUID branchId, String reference, String name, String contact,
 String phone, String email, String status, LocalDate birthDate, LocalDate startDate, LocalDate endDate,
 BigDecimal amount, String currency, LocalDateTime eventDate, String notes, List<CompanyParticipant> participants,UUID ownerId,String ownerName,String nextAction,java.time.Instant dueAt,java.time.Instant lastContactAt,String priority) {}
