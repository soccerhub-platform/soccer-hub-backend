package kz.edu.soccerhub.common.dto.company;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.time.*;
import java.util.UUID;
@Builder
public record FollowUpCommand(@NotNull Operation operation,@NotNull @Min(0) Long version,UUID ownerId,@Size(max=240) String nextAction,Instant dueAt,@Size(max=40) String outcome,LocalDate promisedPaymentDate,@Size(max=2000) String comment) {
 public enum Operation { PLAN, CONTACT, COMPLETE }
}
